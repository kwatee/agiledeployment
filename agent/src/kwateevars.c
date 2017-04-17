#include "ezxml.h"
#include "utils.h"
#include "kwateeerrors.h"
#include "kwateevars.h"

#ifndef _POSIX_SOURCE
#define _POSIX_SOURCE
#endif

#if (defined(WIN))
#include <direct.h>
#endif

# include <errno.h>
# ifdef _WEAK_POSIX
#  define mode_t int
# else
#  include <sys/stat.h>
#  include <unistd.h>
# endif
#include <dirent.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define MAX_LINE_SIZE	1000L

typedef struct variables {
	struct variables *parent;
	char prefixChar;
	int count;
	char nameValues[];
} Variables;

/*************************************************************************
 *
 *************************************************************************/
static int getOSIndependentline(char *buffer, FILE *in) {
	int count = 0;
	int c;
	while ((c = fgetc(in)) != EOF) {
		if (count >= (MAX_LINE_SIZE-1)) {
			ungetc(c, in);
			break;
		}
		buffer[count ++] = (char)c;
		if (c == '\n')
			break;
		if (c == '\r') {
			c = fgetc(in);
			if (c == EOF)
				break;
			if (c != '\n' || count >= (MAX_LINE_SIZE-1))
				ungetc(c, in);
			else if (c == '\n')
				buffer[count ++] = (char)c;
			break;
		}
	}
	buffer[count] = '\0';
	return count;
}

/*************************************************************************
 *
 *************************************************************************/
static char *getVarValue(char *varName, Variables *variables) {
	char *name = variables->nameValues;
	int i;
	for (i = 0; i < variables->count; i ++) {
		char *value = name + (size_t)strlen(name) + 1L;
		if (strcmp(varName, name) == 0)
			return value;
		name = value + (size_t)strlen(value) + 1L;
	}
	if (!variables->parent)
		exitGenericError("Missing variable %s", varName);
	return getVarValue(varName, variables->parent);
}

/*************************************************************************
 *
 *************************************************************************/
static char *extractVar(char *text, size_t *varOffset, size_t *varLen, Variables *variables) {
	char *var = text;
	while((var = strchr(var, variables->prefixChar))) {
		if (var[1] == '{') {
			char * varEnd = strchr(&var[2], '}');
			if (varEnd) {
				*varOffset = (size_t)var - (size_t)text;
				*varLen = (size_t)varEnd - (size_t)var + 1L;
				var += 2;
				int v = 0;
				while (var[v] != '}' && var[v] != '=')
					v ++;
				char *varName = malloc(v + 1);
				memcpy(varName, var, v);
				varName[v] = 0;
				char *varValue = getVarValue(varName, variables);
				free(varName);
				if (varValue)
					return varValue;
			}
		} else
			var ++;
	}
	return NULL;
}

/*************************************************************************
 *
 *************************************************************************/
static void processText(char *text, FILE *out, Variables *variables) {
	while (*text) {
		size_t varOffset;
		size_t varLen;
		char *varValue = extractVar(text, &varOffset, &varLen, variables);
		if (varValue) {
			if (varOffset) {
				if (fwrite(text, varOffset, 1L, out) != 1L)
					exitGenericError("write file", NULL);
			}
			processText(varValue, out, variables);
			text += varOffset + varLen;
		} else {
			if (fwrite(text, (size_t)strlen(text), 1L, out) != 1L)
				exitGenericError("write file", NULL);
			break;
		}
	}
}

/*************************************************************************
 *
 *************************************************************************/
static Variables *parseVariables(ezxml_t xml) {
	if (!xml)
		return NULL;
	ezxml_t vars = ezxml_child(xml, "variables");
	if (!vars)
		return NULL;
	size_t maxSize = 10000L;
	Variables *variables = (Variables *)malloc(maxSize);
	if (!variables)
		exitNoDescriptorError("malloc error", "");
	size_t currentSize = sizeof(Variables);
	variables->parent = NULL;
	variables->count = 0;
	char *prefixChar = (char *)ezxml_attr(vars, "prefixChar");
	if (prefixChar && *prefixChar)
		variables->prefixChar = *prefixChar;
	else
		variables->prefixChar = '%';
	ezxml_t v;
	for (v = ezxml_child(vars, "variable"); v; v = ezxml_next(v)) {
		char *name = (char *)ezxml_attr(v, "name");
		if (name) {
			variables->count ++;
			char *value = ezxml_txt(v);
			size_t s = (size_t)strlen(name) + (size_t)strlen(value) + 2;
			if ((currentSize + s) >= maxSize) {
				maxSize += s + 10000L;
				if (!realloc(&variables, maxSize))
					exitNoDescriptorError("malloc error", "");
			}
			strcpy((char *)variables + currentSize, name);
			strcpy((char *)variables + currentSize + strlen(name) + 1, value);
			currentSize += s;
		}
	}
	return variables;
}

/*************************************************************************
 *
 *************************************************************************/
static void instantiateVariables(FILE *in, FILE *out, Variables *variables) {
	if (variables) {
		char line[MAX_LINE_SIZE+1];
		while (getOSIndependentline(line, in)) {
			processText(line, out, variables);
		}
	}
}

/*************************************************************************
 *
 *************************************************************************/
static void instantiateRecursively(char *srcPath, char *dstPath, Variables *variables) {
	DIR *dir = opendir(srcPath);
	if (dir) {
		struct dirent *ent;
		while ((ent = readdir (dir))) {
			if (strcmp(ent->d_name, ".") && strcmp(ent->d_name, "..")) {
				char *src = concatenatePath(srcPath, ent->d_name);
				struct stat buf;
				if (stat(src, &buf) != -1) {
					char *dst = concatenatePath(dstPath, ent->d_name);
					if (S_ISDIR(buf.st_mode)) {
						instantiateRecursively(src, dst, variables);
					} else {
						FILE *in = fopen(src, "r");
						createfilepath(dst, 1);
						FILE *out = fopen(dst, "w");
						instantiateVariables(in, out, variables);
						fclose(in);
						fclose(out);
					}
					free(dst);
				}
				free(src);
			}
		}
	}
}

/*************************************************************************
 *
 *************************************************************************/
static Variables *loadVariables(char *fileName, char *artifact) {
	if (!fileExists(fileName, 0))
		exitNoDescriptorError("No kwatee variables", "");
	Variables *variables = NULL;
	ezxml_t environment = ezxml_parse_file(fileName);
	if (environment) {
		Variables *envVariables = parseVariables(environment);
		ezxml_t artifacts = ezxml_child(environment, "artifacts");
		if (artifacts) {
			ezxml_t a;
			for (a = ezxml_child(artifacts, "artifact"); a; a = ezxml_next(a)) {
				if (strcmp(ezxml_attr(a, "name"), artifact) == 0) {
					variables = parseVariables(a);
					variables->parent = envVariables;
					break;
				}
			}
		}
		ezxml_free(environment);
	}
	return variables;
}

/*************************************************************************
 *
 *************************************************************************/
void instantiateAllFiles(char *srcDir, char *dstDir, char *variablesFile, char *artifact) {
	Variables *variables = loadVariables(variablesFile, artifact);
	instantiateRecursively(srcDir, dstDir, variables);
	free(variables);
}

