/* utils.c */

#include "utils.h"
#ifndef SEEK_SET
# define SEEK_SET 0
#endif

#ifndef _POSIX_SOURCE
#define _POSIX_SOURCE
#endif

#if (defined(WIN))
#include <direct.h>
#endif

#ifdef _WEAK_POSIX
# ifndef _POSIX_SOURCE
#  define _POSIX_SOURCE
# endif
#endif

#ifdef _POSIX_SOURCE
# include <errno.h>
# include <sys/types.h>
# include <sys/time.h>
# include <utime.h>
# ifdef _WEAK_POSIX
#  define mode_t int
# else
#  include <sys/stat.h>
#  include <unistd.h>
# endif
#endif
#include <dirent.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <stdarg.h>
#if defined(WIN)
#define DIR_SEPARATOR '\\'
#else
#define DIR_SEPARATOR '/'
#endif
#include "kwateeerrors.h"
#include "descriptor.h"


/*************************************************************************
 * Create directory path
 *************************************************************************/
int createpath(const char *dir) {
	char *name = malloc(strlen(dir)+1);
	strcpy(name, dir);
	int i;
	for (i = 0; name[i]; i++) {
		/* If this is a slash, then temporarily replace the '/'
		 * with a '\0' and do a mkdir() on the resulting string.
		 * Ignore errors for now.
		 */
		if (name[i] == '/' && i > 0) {
			name[i] = '\0';
            if (!fileExists(name, 1)) {
#if defined(WIN)
                (void)_mkdir(name);
#else
                if (mkdir(name, 0777)) {
                    debug_print("createpath: failed to create dir '%s' (errno=%d)\n", name, errno);
                    return 0;
                }
#endif
            }
			name[i] = '/';
		}
	}
	free(name);
    return 1;
}

/*************************************************************************
 * Create a file for writing.
* If necessary, create the directories leading up to that file as well
 * EXITS in case of failure
 *************************************************************************/
FILE *createfilepath(const char *name, int isText) {
	FILE	*fp;

	/* first try creating it the easy way */
	fp = fopen(name, isText ? "w" : "wb");
	if (fp)
		return fp;

	/* Else try making all of its directories, and then try creating
	 * the file again.
	 */
	if (!createpath(name))
        exitGenericError("createfilepath: could not create parent directory(ies) of %s\n", name);
	fp = fopen(name, isText ? "w" : "wb");
	if (!fp)
		exitGenericError("createfilepath: could not open file %s", name);
	return fp;
}

/*************************************************************************
 * Create a link, or copy a file.
 * If the file is copied (not linked) then print a warning
 * EXITS in vase of failure
 *************************************************************************/
void linkorcopy(const char *src, const char *dst, int isText, int sym) {

	/* Make sure the destination path exists */
    
#ifndef WIN
    struct stat fst;
    struct stat *fstptr = 0L;
    if (!stat(dst, &fst))
        fstptr = &fst;
#endif

    createpath(dst);
    unlink(dst);

	FILE *fpdst = fopen(dst, isText ? "w" : "wb");
	if (!fpdst)
		exitGenericError("linkorcopy: could not open dst file %s", dst);

	FILE *fpsrc = fopen(src, "rb");
	if (!fpsrc)
		exitGenericError("linkorcopy: could not open src file %s", src);

	char buf[4096];
	/* Copy characters */
	size_t len;
	while ((len = fread(buf, sizeof(char), 4096, fpsrc)) > 0) {
		if (fwrite(buf, len, 1, fpdst) != 1)
			exitGenericError("linkorcopy: error writing to file %s", dst);
	}

#ifndef WIN
    if (!fstptr) {
        if (!fstat(fileno(fpsrc), &fst))
            fstptr = &fst;
    }
    if (fstptr) {
        fchown(fileno(fpdst), fstptr->st_uid, fstptr->st_gid);
        fchmod(fileno(fpdst), fstptr->st_mode);
    }
#endif

	/* Close the files */
	fclose(fpsrc);
	fclose(fpdst);
}

/*************************************************************************
 * Return 1 if the file (or directory when "isDir"=1) "fileName" exists
 *************************************************************************/
int fileExists(const char *fileName, int isDir) {
	struct stat fileInfo;
	if (stat(fileName, &fileInfo) == 0) {
		if (!isDir || S_ISDIR(fileInfo.st_mode))
			return 1;
	}
	return 0;
}

/*************************************************************************
 * Delete the dir "dirPath" and all its contents recursively
 * Returns 0 in case of success
 *************************************************************************/
int recursiveRmdir(const char *dirPath) {
	if (!fileExists(dirPath, 1) || rmdir(dirPath) == 0)
		return 0;

	DIR *dir = opendir(dirPath);
	if (!dir) {
		printf("Failed\n");
		return 1;
	}

	struct dirent *ent;
	int retVal = 0;
	while ((ent = readdir (dir))) {
		if (strcmp(ent->d_name, ".") && strcmp(ent->d_name, "..")) {
			char *f = concatenatePath(dirPath, ent->d_name);
			struct stat buf;
			if (stat(f, &buf) != -1) {
				if (S_ISDIR(buf.st_mode)) {
					if (recursiveRmdir(f)) {
						retVal = 1;
						break;
					}
				} else
					unlink(f);
			}
			free(f);
		}
	}
	closedir (dir);
	if (!retVal)
		retVal = rmdir(dirPath);
	return retVal;
}

/*************************************************************************
 * Move all files from "fromDir" into "destinationDir" recursively
 * Create the destinationDir and subdir
 *************************************************************************/
void copyFiles(const char *fromDir, const char *destinationDir, const char *relPath, int silent, const char *signatures, const char *oldSignatures) {
	char *from = concatenatePath(fromDir, relPath?relPath:"");
	if (fileExists(from, 1)) {
		DIR *dir = opendir(from);
		if (!dir)
			exitGenericError("Could not copy files to %s", destinationDir);
	
		struct dirent *ent;
		while ((ent = readdir (dir))) {
			if (strcmp(ent->d_name, ".") && strcmp(ent->d_name, "..")) {
				char *f = concatenatePath(from, ent->d_name);
				struct stat buf;
				if (stat(f, &buf) != -1) {
					char *nextRelPath = concatenatePath(relPath, ent->d_name);
					char *to = concatenatePath(destinationDir, nextRelPath);
					if (S_ISDIR(buf.st_mode)) {
						char *toDir = concatenatePath(to, "");	/* force adjunction of / at end */
						createpath(toDir);
						free(toDir);
						copyFiles(fromDir, destinationDir, nextRelPath, silent, signatures, oldSignatures);
					} else {
						if (!signatures || fileChanged(nextRelPath, signatures, oldSignatures)) {
							linkorcopy(f, to, 0, 0);
							if (!silent)
								printf("  [++]%s\n", nextRelPath);
						}
					}
					free(to);
					free(nextRelPath);
				}
				free(f);
			}
		}
		closedir(dir);
	}
	free(from);
}

/*************************************************************************
 * 
 *************************************************************************/
void listDirsWithExtension(const char *baseDir, const char *suffix) {
	DIR *dir = opendir(baseDir);
	if (dir) {
		struct dirent *ent;
		while ((ent = readdir (dir))) {
			if (endsWith(ent->d_name, suffix)) {
				char buffer[MAX_PATH_LENGTH];
				strcpy(buffer, ent->d_name);
				buffer[strlen(ent->d_name)-strlen(suffix)] = 0;
				printf("%s ", buffer);
			}
		}
		closedir(dir);
	}
	printf("\n");
}

/*************************************************************************
 * Concate two strings into a malloc'ed result
 *************************************************************************/
char *concatenate(const char *str1, const char *str2) {
	char *result = malloc(strlen(str1)+strlen(str2)+1);
	strcpy(result, str1);
	strcat(result, str2);
	return result;
}

/*************************************************************************
 * Concate a dir and a file name into a malloc'ed result
 * If the directory name does not end with the system's default directory
 * separator then add it.
 *************************************************************************/
char *concatenatePath(const char *dir, const char *file) {
	char *result;
	if (!dir) {
		result = malloc(strlen(file)+1);
		*result = 0;
	} else {
		int len = strlen(dir);
		char dirSep = 0;
		if (len == 0 || dir[len-1] != DIR_SEPARATOR)
			dirSep = DIR_SEPARATOR;
	
		result = malloc(len+strlen(file)+(dirSep?1:0)+1);
		strcpy(result, dir);
		if (dirSep) {
			result[len] = dirSep;
			result[len+1] = 0;
		}
	}
	strcat(result, file);
	return result;
}

/*************************************************************************
 * Create a temporary directory in the system's default tmp dir
 *************************************************************************/
char *createTmpDir(char *rootDir) {
    char *envTmpDir = concatenatePath(rootDir, "_tmp");
	/*
	 * Get timestamp in millisecs
	 */
	struct timeval tv;
	gettimeofday(&tv, NULL);
	long ms = tv.tv_sec * 1000L + tv.tv_usec / 1000;
	char dirName[MAX_PATH_LENGTH];
	sprintf(dirName, "%lu%c", ms, DIR_SEPARATOR);
	char *tmpDir = concatenatePath(envTmpDir, dirName);
	recursiveRmdir(tmpDir);
	createpath(tmpDir);		 
	return tmpDir;
}

int endsWith(const char *str, const char *suffix) {
	if (str && suffix) {
		size_t lenstr = strlen(str);
		size_t lensuffix = strlen(suffix);
		if (lensuffix <= lenstr)
			return strncmp(str + lenstr - lensuffix, suffix, lensuffix) == 0;
	}
	return 0;
}

#if (defined(WIN))
/* getline.c -- Replacement for GNU C library function getline

Copyright (C) 1993 Free Software Foundation, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.  */

/* Written by Jan Brittenson, bson@gnu.ai.mit.edu.  */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <sys/types.h>
#include <stdio.h>
#include <assert.h>
#include <errno.h>

/* Always add at least this many bytes when extending the buffer.  */
#define MIN_CHUNK 64

/* Read up to (and including) a TERMINATOR from STREAM into *LINEPTR
   + OFFSET (and null-terminate it). *LINEPTR is a pointer returned from
   malloc (or NULL), pointing to *N characters of space.  It is realloc'd
   as necessary.  Return the number of characters read (not including the
   null terminator), or -1 on error or EOF.  On a -1 return, the caller
   should check feof(), if not then errno has been set to indicate
   the error.  */

int
getstr (lineptr, n, stream, terminator, offset)
     char **lineptr;
     size_t *n;
     FILE *stream;
     char terminator;
     int offset;
{
  int nchars_avail;		/* Allocated but unused chars in *LINEPTR.  */
  char *read_pos;		/* Where we're reading into *LINEPTR. */
  int ret;

  if (!lineptr || !n || !stream)
    {
      errno = EINVAL;
      return -1;
    }

  if (!*lineptr)
    {
      *n = MIN_CHUNK;
      *lineptr = malloc (*n);
      if (!*lineptr)
	{
	  errno = ENOMEM;
	  return -1;
	}
    }

  nchars_avail = *n - offset;
  read_pos = *lineptr + offset;

  for (;;)
    {
      int save_errno;
      register int c = getc (stream);

      save_errno = errno;

      /* We always want at least one char left in the buffer, since we
	 always (unless we get an error while reading the first char)
	 NUL-terminate the line buffer.  */

      assert((*lineptr + *n) == (read_pos + nchars_avail));
      if (nchars_avail < 2)
	{
	  if (*n > MIN_CHUNK)
	    *n *= 2;
	  else
	    *n += MIN_CHUNK;

	  nchars_avail = *n + *lineptr - read_pos;
	  *lineptr = realloc (*lineptr, *n);
	  if (!*lineptr)
	    {
	      errno = ENOMEM;
	      return -1;
	    }
	  read_pos = *n - nchars_avail + *lineptr;
	  assert((*lineptr + *n) == (read_pos + nchars_avail));
	}

      if (ferror (stream))
	{
	  /* Might like to return partial line, but there is no
	     place for us to store errno.  And we don't want to just
	     lose errno.  */
	  errno = save_errno;
	  return -1;
	}

      if (c == EOF)
	{
	  /* Return partial line, if any.  */
	  if (read_pos == *lineptr)
	    return -1;
	  else
	    break;
	}

      *read_pos++ = c;
      nchars_avail--;

      if (c == terminator)
	/* Return the line.  */
	break;
    }

  /* Done - NUL terminate and return the number of chars read.  */
  *read_pos = '\0';

  ret = read_pos - (*lineptr + offset);
  return ret;
}

int
getline (lineptr, n, stream)
     char **lineptr;
     size_t *n;
     FILE *stream;
{
  return getstr (lineptr, n, stream, '\n', 0);
}
#endif

static char debug_mode = 0;
void init_debug_mode() {
    if (getenv("KWATEE_DEBUG")) {
        debug_mode = *getenv("KWATEE_DEBUG");
        debug_print("debug mode ON\n");
    }
}

void debug_print(char *fmt, ...) {
    if (debug_mode) {
        va_list argptr;
        va_start(argptr, fmt);
        printf("DEBUG - ");
        vprintf(fmt, argptr);
        va_end(argptr);
    }
}
