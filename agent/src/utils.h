/* utils.h */

#include <stdio.h>

#if defined(WIN) && !defined(WEXITSTATUS)
	#define WEXITSTATUS(_xx)	(_xx)
#endif

extern int createpath(const char *name);
extern FILE *createfilepath(const char *name, int isText);
extern void linkorcopy(const char *src, const char *dst, int isText, int sym);
extern void appendPath(char *path, const char *file);
extern int fileExists(const char *fileName, int isDir);
extern int recursiveRmdir(const char *dir_path);
extern void copyFiles(const char *fromDir, const char *destinationDir, const char *path, int silent, const char *signatures, const char *oldSignatures);
extern char *createTmpDir(char *rootDir);
extern char *concatenate(const char *str1, const char *str2);
extern char *concatenatePath(const char *dir, const char *file);
extern int endsWith(const char *str, const char *suffix);
extern void listDirsWithExtension(const char *baseDir, const char *suffix);
#if (defined(WIN))
extern int getline (char **lineptr, size_t *n, FILE *stream);
#endif

#define MAX_PATH_LENGTH 4096

extern void init_debug_mode();
extern void debug_print(char *fmt, ...);

