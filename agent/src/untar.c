/* untar.c */

/* DESCRIPTION:
 *	Untar extracts files from an uncompressed tar archive, or one which
 *	has been compressed with gzip. Usually such archives will have file
 *	names that end with ".tar" or ".tgz" respectively, although untar
 *	doesn't depend on any naming conventions.  For a summary of the
 *	command-line options, run untar with no arguments.
 *
 * HOW TO COMPILE:
 *	Untar doesn't require any special libraries or compile-time flags.
 *	A simple "cc untar.c -o untar" (or the local equivalent) is
 *	sufficient.  Even "make untar" works, without needing a Makefile.
 *	For Microsoft Visual C++, the command is "cl /D_WEAK_POSIX untar.c"
 *	(for 32 bit compilers) or "cl /F 1400 untar.c" (for 16-bit).
 *
 *	IF YOU SEE COMPILER WARNINGS, THAT'S NORMAL; you can ignore them.
 *	Most of the warnings could be eliminated by adding #include <string.h>
 *	but that isn't portable -- some systems require <strings.h> and
 *	<malloc.h>, for example.  Because <string.h> isn't quite portable,
 *	and isn't really necessary in the context of this program, it isn't
 *	included.
 *
 * PORTABILITY:
 *	Untar only requires the <stdio.h> header.  It uses old-style function
 *	definitions.  It opens all files in binary mode.  Taken together,
 *	this means that untar should compile & run on just about anything.
 *
 *	If your system supports the POSIX chmod(2), utime(2), link(2), and
 *	symlink(2) calls, then you may wish to compile with -D_POSIX_SOURCE,
 *	which will enable untar to use those system calls to restore the
 *	timestamp and permissions of the extracted files, and restore links.
 *	(For Linux, _POSIX_SOURCE is always defined.)
 *
 *	For systems which support some POSIX features but not enough to support
 *	-D_POSIX_SOURCE, you might be able to use -D_WEAK_POSIX.  This allows
 *	untar to restore time stamps and file permissions, but not links.
 *	This should work for Microsoft systems, and hopefully others as well.
 *
 * AUTHOR & COPYRIGHT INFO:
 *	Written by Steve Kirkendall, kirkenda@cs.pdx.edu
 *	Placed in public domain, 6 October 1995
 *
 *	Portions derived from inflate.c -- Not copyrighted 1992 by Mark Adler
 *	version c10p1, 10 January 1993
 */

#include "untar.h"
#include <stdio.h>
#include <stdlib.h>
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
# include <utime.h>
# ifdef _WEAK_POSIX
#  define mode_t int
# else
#  include <sys/stat.h>
#  include <unistd.h>
# endif
#endif
#include <stdlib.h>
#include <string.h>
#include "utils.h"
#include "kwateeerrors.h"
#include "descriptor.h"


#define WSIZE	32768L	/* size of decompression buffer */
#define TSIZE	512L	/* size of a "tape" block */
#define CR	13	/* carriage-return character */
#define LF	10	/* line-feed character */

typedef unsigned char	Uchar_t;
typedef unsigned short	Ushort_t;
typedef unsigned long	Ulong_t;

typedef struct
{
	Uchar_t	magic[2];	/* magic: 0x1F, 0x8b */
	Uchar_t	compression;	/* compression method: 8=deflated */
	Uchar_t	flags;		/* content flags: 0x08 bit -> name present */
	Uchar_t	mtime[4];	/* time_t when archive created */
	Uchar_t	extraflags;	/* ? */
	Uchar_t	os;		/* operating system: 3=UNIX */
	/* if flags&0x08, then original file name goes here, '\0'-terminated */
} gzhdr_t;
#define MAGIC0	0x1f
#define MAGIC1	0x8b
#define DEFLATE	0x08
#define NAME	0x08

typedef struct
{
	char	filename[100];	/*   0  name of next file */
	char	mode[8];	/* 100  Permissions and type (octal digits) */
	char	owner[8];	/* 108  Owner ID (ignored) */
	char	group[8];	/* 116  Group ID (ignored) */
	char	size[12];	/* 124  Bytes in file (octal digits) */
	char	mtime[12];	/* 136  Modification time stamp (octal digits)*/
	char	checksum[8];	/* 148  Header checksum (ignored) */
	char	type;		/* 156  File type (see below) */
	char	linkto[100];	/* 157  Linked-to name */
	char	brand[8];	/* 257  Identifies tar version (ignored) */
	char	ownername[32];	/* 265  Name of owner (ignored) */
	char	groupname[32];	/* 297  Name of group (ignored) */
	char	devmajor[8];	/* 329  Device major number (ignored) */
	char	defminor[8];	/* 337  Device minor number (ignored) */
	char	prefix[155];	/* 345  Prefix of name (optional) */
	char	RESERVED[12];	/* 500  Pad header size to 512 bytes */
} tar_t;
#define ISREGULAR(hdr)	((hdr).type < '1' || (hdr).type > '6')
#define ISLONGFILENAME(hdr)	((hdr).type == 'K' || (hdr).type == 'L')

typedef struct huft {
	Uchar_t e;	/* number of extra bits or operation */
	Uchar_t b;	/* number of bits in this code or subcode */
	union {
		Ushort_t	n; /* literal, length base, or distance base */
		struct huft	*t;/* pointer to next level of table */
	} v;
} huft_t;

int wp;	/* output counter */
Uchar_t slide[WSIZE];

/* Tables for deflate from PKZIP's appnote.txt. */
static unsigned border[] = {	/* Order of the bit length code lengths */
		16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
static Ushort_t cplens[] = {	/* Copy lengths for literal codes 257..285 */
		3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
		35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0};
/* note: see note #13 above about the 258 in this list. */
static Ushort_t cplext[] = {	/* Extra bits for literal codes 257..285 */
		0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
		3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 99, 99}; /* 99==invalid */
static Ushort_t cpdist[] = {	/* Copy offsets for distance codes 0..29 */
		1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
		257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
		8193, 12289, 16385, 24577};
static Ushort_t cpdext[] = {	 /* Extra bits for distance codes */
		0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
		7, 7, 8, 8, 9, 9, 10, 10, 11, 11,
		12, 12, 13, 13};


static char *s_signatures;
static char *s_oldSignatures;
static char	*s_basedir;/* base directory for extraction */
static char	*s_inname;/* name of input archive */
static FILE	*s_infp;	/* input byte stream */
static FILE	*s_outfp;	/* output stream, for file currently being extracted */
static Ulong_t	s_out_size;/* number of bytes remainin in file currently being extracted */
static FILE	*s_tarfp;	/* usually NULL; else file for writing gunzipped data */
static int	s_maketar;/* -d: 1 to gunzip only, 0 to gunzip and extract tar files */
static int	quiet;	/* -q: 1 to write nothing to stdout, 0 for normal chatter */
static int	s_is_abs_path;/* -p: 1 to allow leading '/', 0 to strip leading '/' */
static int	s_convert_newlines;/* -c: 1 to convert newlines, 0 to leave unchanged */
static int	s_ignore_gzip_name;	/* -n: 1 to ignore gzip name, 0 to use gzip name */
static char	**s_files_to_extract;	/* array of filenames to extract/list */
static int s_num_files_to_extract;
static int	s_didabs;	/* were any filenames affected by the absence of -p? */
static int file_uid;
static int file_gid;

static Ulong_t s_bit_buffer; 	/* bit buffer */
static unsigned s_bits_in_buffer;   	/* bits in bit buffer */

static Ushort_t s_mask_bits[] = {
		0x0000,
		0x0001, 0x0003, 0x0007, 0x000f, 0x001f, 0x003f, 0x007f, 0x00ff,
		0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff, 0x3fff, 0x7fff, 0xffff
};

static char s_long_file_buffer[2000];
static char * s_long_file_name = NULL;

#define NEXTBYTE()  (Uchar_t)getc(s_infp)
#define NEEDBITS(n) {while(k<(n)){b|=((Ulong_t)NEXTBYTE())<<k;k+=8;}}
#define DUMPBITS(n) {b>>=(n);k-=(n);}

static int s_lbits = 9;	/* bits in base literal/length lookup table */
static int s_dbits = 6;	/* bits in base distance lookup table */


/* If BMAX needs to be larger than 16, then h and x[] should be Ulong_t. */
#define BMAX	16	 /* maximum bit length of any code (16 for explode) */
#define N_MAX	288	 /* maximum number of codes in any set */


static unsigned s_hufts;		 /* track memory usage */


/* This calls fwrite(), possibly after converting CR-LF to LF */
static void cvtwrite(blk, size, fp)
Uchar_t	*blk;	/* the block to be written */
Ulong_t	size;	/* number of characters to be written */
FILE	*fp;	/* file to write to */
{
	int	i, j;
	static Uchar_t mod[TSIZE];

	if (s_convert_newlines)
	{
		for (i = j = 0; i < size; i++)
		{
			/* convert LF to local newline convention */
			if (blk[i] == LF)
				mod[j++] = '\n';
			/* If CR-LF pair, then delete the CR */
			else if (blk[i] == CR && (i+1 >= size || blk[i+1] == LF))
				continue;
			else /* other characters copied literally */
				mod[j++] = blk[i];
		}
		size = j;
		blk = mod;
	}

	if (fwrite(blk, (size_t)size, 1, fp) != 1) {
		char buf[10];
		sprintf(buf, "%d", errno);
		exitGenericError("write failed[errno=%s]", buf);
	}
}


/* Compute the checksum of a tar header block, and return it as a long int.
 * The checksum can be computed using either POSIX rules (unsigned bytes)
 * or Sun rules (signed bytes).
 */
static long checksum(tar_t *tblk, int sunny) {
	long	sum;
	char	*scan;

	/* compute the sum of the first 148 bytes -- everything up to but not
	 * including the checksum field itself.
	 */
	sum = 0L;
	for (scan = (char *)tblk; scan < tblk->checksum; scan++) {
		sum += (*scan) & 0xff;
		if (sunny && (*scan & 0x80) != 0)
			sum -= 256;
	}

	/* for the 8 bytes of the checksum field, add blanks to the sum */
	sum += ' ' * sizeof tblk->checksum;
	scan += sizeof tblk->checksum;

	/* finish counting the sum of the rest of the block */
	for (; scan < (char *)tblk + sizeof *tblk; scan++) {
		sum += (*scan) & 0xff;
		if (sunny && (*scan & 0x80) != 0)
			sum -= 256;
	}

	return sum;
}



/* list files in an archive, and optionally extract them as well */
static void untar(Uchar_t	*blk) {
	static char	nbuf[4000];/* storage space for prefix+name, combined */
	static char	*name,*n2;/* prefix and name, combined */
	static int	first = 1;/* Boolean: first block of archive? */
	long		sum;	  /* checksum for this block */
	int		i;
	tar_t		tblk[1];
	char *relativeFilePath;

#ifdef _POSIX_SOURCE
	static mode_t		mode;		/* file permissions */
	static struct utimbuf	timestamp;	/* file timestamp */
#endif

	/* make a local copy of the block, and treat it as a tar header */
	tblk[0] = *(tar_t *)blk;

	/* process each type of tape block differently */
	if (s_out_size > TSIZE) {
		/* data block, but not the last one */
		if (s_outfp)
			cvtwrite(blk, (Ulong_t)TSIZE, s_outfp);
		else if (s_long_file_name) {
			memcpy(s_long_file_name, blk, (Ulong_t)TSIZE);
			s_long_file_name += TSIZE;
		}

		s_out_size -= TSIZE;
	} else if (s_out_size > 0) {
		/* last data block of current file */
		if (s_outfp) {
			cvtwrite(blk, s_out_size, s_outfp);
			fflush(s_outfp);
			fclose(s_outfp);
			s_outfp = NULL;
#ifdef _POSIX_SOURCE
			if (timestamp.modtime)
				utime(nbuf, &timestamp);
            if (mode)
                mode &= 07777;
            else
                mode = 0644;
            chmod(nbuf, mode);
#endif
			if (s_num_files_to_extract > 0)
				s_num_files_to_extract --;
		} else if (s_long_file_name) {
			memcpy(s_long_file_name, blk, s_out_size);
			s_long_file_name[s_out_size] = '\0';
			s_long_file_name = s_long_file_buffer;
		}
		s_out_size = 0;
	} else if ((tblk)->filename[0] == '\0') {
		/* end-of-archive marker */
		if (s_didabs)
			fprintf(stderr, "WARNING: Removed leading slashes because \"-p\" wasn't given.\n");
		return;
	} else {
		if (s_num_files_to_extract == 0) {
			// special case when s_files_to_extract is non NULL: we check if the file name matches the extension in s_files_to_extract and print it if yes
			int extensionLen = strlen(s_files_to_extract[0]);
			int len = strlen((tblk)->filename);
			if (len >= extensionLen && strcmp((tblk)->filename+len-extensionLen, s_files_to_extract[0]) == 0) {
				// print file name without the extension
				char c = (tblk)->filename[len-extensionLen];
				(tblk)->filename[len-extensionLen] = 0;
				printf("  %s\n", (tblk)->filename);
				(tblk)->filename[len-extensionLen] = c;
			}
			return;
		}

		/* file header */
		/* half-assed verification -- does it look like header? */
		if ((tblk)->filename[99] != '\0' || ((tblk)->size[0] < '0' && (tblk)->size[0] != ' ') || (tblk)->size[0] > '9') {
			if (first)
				exitGenericError("not a valid tar file", 0L);
		}

		/* verify the checksum */
		for (sum = 0L, i = 0; i < sizeof((tblk)->checksum); i++) {
			if ((tblk)->checksum[i] >= '0' && (tblk)->checksum[i] <= '7')
				sum = sum * 8 + (tblk)->checksum[i] - '0';
		}
		if (sum != checksum(tblk, 0) && sum != checksum(tblk, 1)) {
			fflush(stdout);
			exitGenericError("header has bad checksum %s", (tblk)->filename);
		}

		/* From this point on, we don't care whether this is the first
		 * block or not.  Might as well reset the "first" flag now.
		 */
		first = 0;

		/* convert file size */
		for (s_out_size = 0L, i = 0; i < sizeof((tblk)->size); i++) {
			if ((tblk)->size[i] >= '0' && (tblk)->size[i] <= '7')
				s_out_size = s_out_size * 8 + (tblk)->size[i] - '0';
		}

		if (ISLONGFILENAME(*tblk)) {
			if (s_out_size >= sizeof s_long_file_buffer)
				exitGenericError("File name exceeds 4000 characters", 0L);
			s_outfp = 0L;
			s_long_file_name = s_long_file_buffer;
			return;
		}

		/* combine prefix and filename */
		memset(nbuf, 0, sizeof nbuf);
		name = nbuf;
		strcpy(name, s_basedir);
		int l = strlen(s_basedir)-1;
		if (*s_basedir != '\0' && s_basedir[l] != '/' && s_basedir[l] != '\\')
			strcat(name, "/");
		relativeFilePath = name+strlen(name);
		if ((tblk)->prefix[0]) {
			strncat(name, (tblk)->prefix, sizeof (tblk)->prefix);
			strcat(name, "/");
			/* If s_long_file_name is not NULL we got a @LongName in the previous iteration which gave us the file name */
			if (s_long_file_name)
				strncat(name + strlen(name), s_long_file_name, sizeof s_long_file_buffer);
			else
				strncat(name + strlen(name), (tblk)->filename, sizeof (tblk)->filename);
		} else {
			/* If s_long_file_name is not NULL we got a @LongName in the previous iteration which gave us the file name */
			if (s_long_file_name)
				strncat(name, s_long_file_name, sizeof s_long_file_buffer);
			else
				strncat(name, (tblk)->filename, sizeof (tblk)->filename);
		}
		s_long_file_name = NULL;
		/* Convert any backslashes to forward slashes, and guard
		 * against doubled-up slashes. (Some DOS versions of "tar"
		 * get this wrong.)  Also strip off leading slashes.
		 */
		if (!s_is_abs_path && (*name == '/' || *name == '\\'))
			s_didabs = 1;
		for (n2 = nbuf; *name; name++) {
			if (*name == '\\')
				*name = '/';
			if (*name != '/' || (s_is_abs_path && n2 == nbuf) || (n2 != nbuf && n2[-1] != '/'))
				*n2++ = *name;
		}
		if (n2 == nbuf)
			*n2++ = '/';
		*n2 = '\0';

		/* if last character of name is '/' then assume directory */
		if (*nbuf && nbuf[strlen(nbuf) - 1] == '/')
			(tblk)->type = '5';



#ifdef _POSIX_SOURCE
		/* convert file timestamp */
		for (timestamp.modtime=0L, i=0; i < sizeof((tblk)->mtime); i++) {
			if ((tblk)->mtime[i] >= '0' && (tblk)->mtime[i] <= '7')
				timestamp.modtime = timestamp.modtime * 8 + (tblk)->mtime[i] - '0';
		}
		timestamp.actime = timestamp.modtime;

		/* convert file permissions */
		for (mode = i = 0; i < sizeof((tblk)->mode); i++) {
			if ((tblk)->mode[i] >= '0' && (tblk)->mode[i] <= '7')
				mode = mode * 8 + (tblk)->mode[i] - '0';
		}
//        printf("***mode=%o file=%s\n", mode, nbuf);
#endif

		/* If we have an "only" list, and this file isn't in it,
		 * then skip it.
		 */
		if (s_files_to_extract) {
			for (i = 0; s_files_to_extract[i]; i ++) {
				int l = strlen(s_files_to_extract[i]);
				if (s_files_to_extract[i][l-1] == '/') {
					/* match all subfiles/subdirs */
					if (strncmp(s_files_to_extract[i], relativeFilePath, l) == 0)
						break;
				} else {
					if (strcmp(s_files_to_extract[i], relativeFilePath) == 0)
						break;
				}
			}
			if (!s_files_to_extract[i]) {
				s_outfp = NULL;
				return;
			}
		}

		if (!fileChanged(relativeFilePath, s_signatures, s_oldSignatures)) {
			s_outfp = NULL;
			return;
		}

		/* list the file */
		if (!quiet && relativeFilePath[strlen(relativeFilePath)-1] != '/')
            printf("  [+] %s\n", relativeFilePath);

		/* if link, then do the link-or-copy thing */
		if (tblk->type == '1' || tblk->type == '2') {
			linkorcopy(tblk->linkto, nbuf, s_convert_newlines, tblk->type == '2');
			s_out_size = 0L;
			return;
		}

		/* If directory, then make a weak attempt to create it.
		 * Ideally we would do the "create path" thing, but that
		 * seems like more trouble than it's worth since traditional
		 * tar archives don't contain directories anyway.
		 */
		if (tblk->type == '5') {
            if (createpath(nbuf) == 0) {
				n2 = " created";
#ifdef _POSIX_SOURCE
                if (timestamp.modtime)
                    utime(nbuf, &timestamp);
                if (mode)
                    mode &= 07777;
                else
                    mode = 0755;
                chmod(nbuf, mode);
#endif
            } else
				n2 = " ignored";
			return;
		}

		/* if not a regular file, then skip it */
		if (!ISREGULAR(*tblk)) {
			printf("DEBUG: irregular\n");
			s_out_size = 0L;
			return;
		}

		/* if extracting, then try to create the file */
		s_outfp = createfilepath(nbuf, s_convert_newlines);

		/* if file is 0 bytes long, then we're done already! */
		if (s_out_size == 0 && s_outfp) {
			fclose(s_outfp);
#ifdef _POSIX_SOURCE
			if (timestamp.modtime)
				utime(nbuf, &timestamp);
			if (mode)
                mode &= 07777;
            else
                mode = 0644;
            chmod(nbuf, mode);
#endif
		}
	}
}

/* send decompressed tape blocks to untar() */
static void flush_output(unsigned w) {
	unsigned	i;

	if (s_tarfp)
		cvtwrite(slide, (Ulong_t)w, s_tarfp);
	else {
		/* send each block to untar() */
		for (i = 0; i + TSIZE <= w; i += TSIZE)
			untar(&slide[i]);
	}
}

/*----------------------------------------------------------------------------*/

/* Given a list of code lengths and a maximum table size, make a set of
 * tables to decode that set of codes.	Return zero on success, one if
 * the given code set is incomplete (the tables are still built in this
 * case), two if the input is invalid (all zero length codes or an
 * oversubscribed set of lengths), and three if not enough memory.
 */
static int huft_build(b, n, s, d, e, t, m)
unsigned	*b;	/* code lengths in bits (all assumed <= BMAX) */
unsigned	n;	/* number of codes (assumed <= N_MAX) */
unsigned	s;	/* number of simple-valued codes (0..s-1) */
Ushort_t	*d;	/* list of base values for non-simple codes */
Ushort_t	*e;	/* list of extra bits for non-simple codes */
huft_t		**t;	/* result: starting table */
int		*m;	/* maximum lookup bits, returns actual */
{
	unsigned	  a;		/* counter for codes of length k */
	unsigned	  c[BMAX+1];	/* bit length count table */
	unsigned	  f;		/* i repeats in table every f entries */
	int		  g;		/* maximum code length */
	int		  h;		/* table level */
	register unsigned i;		/* counter, current code */
	register unsigned j;		/* counter */
	register int	  k;		/* number of bits in current code */
	int		  l;		/* bits per table (returned in m) */
	register unsigned *p;		/* pointer into c[], b[], or v[] */
	register huft_t   *q;		/* points to current table */
	huft_t		  r;		/* table entry for structure assignment */
	huft_t		  *u[BMAX];	/* table stack */
	unsigned	  v[N_MAX];	/* values in order of bit length */
	register int	  w;		/* bits before this table == (l * h) */
	unsigned	  x[BMAX+1];	/* bit offsets, then code stack */
	unsigned	  *xp;		/* pointer into x */
	int		  y;		/* number of dummy codes added */
	unsigned	  z;		/* number of entries in current table */


	/* Generate counts for each bit length */
	memset(c, 0, sizeof(c));
	p = b;	i = n;
	do {
		c[*p++]++;	/* assume all entries <= BMAX */
	} while (--i);
	if (c[0] == n) {		/* null input--all zero length codes */
		*t = (huft_t *)NULL;
		*m = 0;
		return 0;
	}


	/* Find minimum and maximum length, bound *m by those */
	l = *m;
	for (j = 1; j <= BMAX; j++) {
		if (c[j])
			break;
	}
	k = j;		/* minimum code length */
	if ((unsigned)l < j)
		l = j;
	for (i = BMAX; i; i--) {
		if (c[i])
			break;
	}
	g = i;		/* maximum code length */
	if ((unsigned)l > i)
		l = i;
	*m = l;


	/* Adjust last length count to fill out codes, if needed */
	for (y = 1 << j; j < i; j++, y <<= 1) {
		if ((y -= c[j]) < 0)
			return 2;	/* bad input: more codes than bits */
	}
	if ((y -= c[i]) < 0)
		return 2;
	c[i] += y;


	/* Generate starting offsets into the value table for each length */
	x[1] = j = 0;
	p = c + 1;	xp = x + 2;
	while (--i) {			 /* note that i == g from above */
		*xp++ = (j += *p++);
	}

	/* Make a table of values in order of bit lengths */
	p = b;	i = 0;
	do {
		if ((j = *p++) != 0)
			v[x[j]++] = i;
	} while (++i < n);


	/* Generate the Huffman codes and for each, make the table entries */
	x[0] = i = 0;	/* first Huffman code is zero */
	p = v;		/* grab values in bit order */
	h = -1;		/* no tables yet--level -1 */
	w = -l;		/* bits decoded == (l * h) */
	u[0] = (huft_t *)NULL;	/* just to keep compilers happy */
	q = (huft_t *)NULL;	/* ditto */
	z = 0;			/* ditto */

	/* go through the bit lengths (k already is bits in shortest code) */
	for (; k <= g; k++) {
		a = c[k];
		while (a--) {
			/* here i is the Huffman code of length k bits for value *p */
			/* make tables up to required level */
			while (k > w + l) {
				h++;
				w += l;	/* previous table always l bits */

				/* compute minimum size table less than or equal to l bits */
				z = (z = g - w) > (unsigned)l ? l : z;	/* upper limit on table size */
				if ((f = 1 << (j = k - w)) > a + 1)	{ /* try a k-w bit table */
					/* too few codes for k-w bit table */
					f -= a + 1;	 /* deduct codes from patterns left */
					xp = c + k;
					while (++j < z) {	 /* try smaller tables up to z bits */
						if ((f <<= 1) <= *++xp)
							break;						/* enough codes to use up j bits */
						f -= *xp;	/* else deduct codes from patterns */
					}
				}
				z = 1 << j;						 /* table entries for j-bit table */

				/* allocate and link in new table */
				q = (huft_t *)malloc((z + 1)*sizeof(huft_t));
				s_hufts += z + 1;	 /* track memory usage */
				*t = q + 1;						 /* link to list for huft_free() */
				*(t = &(q->v.t)) = (huft_t *)NULL;
				u[h] = ++q;						 /* table starts after link */

				/* connect to last table, if there is one */
				if (h) {
					x[h] = i;		/* save pattern for backing up */
					r.b = (Uchar_t)l;	/* bits to dump before this table */
					r.e = (Uchar_t)(16 + j);/* bits in this table */
					r.v.t = q;		/* pointer to this table */
					j = i >> (w - l);	/* (get around Turbo C bug) */
					u[h-1][j] = r;		/* connect to last table */
				}
			}

			/* set up table entry in r */
			r.b = (Uchar_t)(k - w);
			if (p >= v + n)
				r.e = 99;/* out of values--invalid code */
			else if (*p < s) {
				r.e = (Uchar_t)(*p < 256 ? 16 : 15);/* 256 is end-of-block code */
				r.v.n = *p++;			 /* simple code is just the value */
			} else {
				r.e = (Uchar_t)e[*p - s];	 /* non-simple--look up in lists */
				r.v.n = d[*p++ - s];
			}

			/* fill code-like entries with r */
			f = 1 << (k - w);
			for (j = i >> w; j < z; j += f)
				q[j] = r;

			/* backwards increment the k-bit code i */
			for (j = 1 << (k - 1); i & j; j >>= 1)
				i ^= j;
			i ^= j;

			/* backup over finished tables */
			while ((i & ((1 << w) - 1)) != x[h]) {
				h--;		/* don't need to update q */
				w -= l;
			}
		}
	}


	/* Return true (1) if we were given an incomplete table */
	return y != 0 && g != 1;
}



/* Free the malloc'ed tables built by huft_build(), which makes a linked
 * list of the tables it made, with the links in a dummy first entry of
 * each table.
 */
static int huft_free(huft_t *t) {
	register huft_t *p, *q;


	/* Go through linked list, freeing from the malloced (t[-1]) address. */
	p = t;
	while (p != (huft_t *)NULL) {
		q = (--p)->v.t;
		free(p);
		p = q;
	}
	return 0;
}


/* Inflate (decompress) the codes in a deflated (compressed) block.
 * Return an error code or zero if it all goes ok.
 */
static int inflate_codes(huft_t *tl, huft_t *td, int bl, int bd) {
	register unsigned e;		/* table entry flag/number of extra bits */
	unsigned	  n, d;		/* length and index for copy */
	unsigned	  w;		/* current window position */
	huft_t		  *t;		/* pointer to table entry */
	unsigned	  ml, md;	/* masks for bl and bd bits */
	register Ulong_t  b;		/* bit buffer */
	register unsigned k;		/* number of bits in bit buffer */


	/* make local copies of globals */
	b = s_bit_buffer;	/* initialize bit buffer */
	k = s_bits_in_buffer;
	w = wp;	/* initialize window position */

	/* inflate the coded data */
	ml = s_mask_bits[bl]; /* precompute masks for speed */
	md = s_mask_bits[bd];
	for (;;) {	/* do until end of block */
		NEEDBITS((unsigned)bl)
				if ((e = (t = tl + ((unsigned)b & ml))->e) > 16) {
					do {
						if (e == 99)
							return 1;
						DUMPBITS(t->b)
						e -= 16;
						NEEDBITS(e)
					} while ((e = (t = t->v.t + ((unsigned)b & s_mask_bits[e]))->e) > 16);
				}
		DUMPBITS(t->b)
		if (e == 16) {	/* then it's a literal */
			slide[w++] = (Uchar_t)t->v.n;
			if (w == WSIZE) {
				flush_output(w);
				if (s_num_files_to_extract == 0)
					return 1;
				w = 0;
			}
		} else {	/* it's an EOB or a length */
			/* exit if end of block */
			if (e == 15)
				break;

			/* get length of block to copy */
			NEEDBITS(e)
			n = t->v.n + ((unsigned)b & s_mask_bits[e]);
			DUMPBITS(e);

			/* decode distance of block to copy */
			NEEDBITS((unsigned)bd)
			if ((e = (t = td + ((unsigned)b & md))->e) > 16) {
				do {
					if (e == 99)
						return 1;
					DUMPBITS(t->b)
					e -= 16;
					NEEDBITS(e)
				} while ((e = (t = t->v.t + ((unsigned)b & s_mask_bits[e]))->e) > 16);
			}
			DUMPBITS(t->b)
			NEEDBITS(e)
			d = w - t->v.n - ((unsigned)b & s_mask_bits[e]);
			DUMPBITS(e)

			/* do the copy */
			do {
				n -= (e = (e = WSIZE - ((d &= WSIZE-1) > w ? d : w)) > n ? n : e);
#if !defined(NOMEMCPY) && !defined(DEBUG)
				if (w - d >= e) {	 /* (this test assumes unsigned comparison) */
					memcpy(slide + w, slide + d, e);
					w += e;
					d += e;
				}
				else	/* do it slow to avoid memcpy() overlap */
#endif /* !NOMEMCPY */
					do {
						slide[w++] = slide[d++];
					} while (--e);
				if (w == WSIZE) {
					flush_output(w);
					if (s_num_files_to_extract == 0)
						return 1;
					w = 0;
				}
			} while (n);
		}
	}

	/* restore the globals from the locals */
	wp = w;	/* restore global window pointer */
	s_bit_buffer = b;	/* restore global bit buffer */
	s_bits_in_buffer = k;

	/* done */
	return 0;
}



/* "decompress" an inflated type 0 (stored) block. */
static int inflate_stored() {
	unsigned	  n;	/* number of bytes in block */
	unsigned	  w;	/* current window position */
	register Ulong_t  b;	/* bit buffer */
	register unsigned k;	/* number of bits in bit buffer */

	/* make local copies of globals */
	b = s_bit_buffer;	/* initialize bit buffer */
	k = s_bits_in_buffer;
	w = wp;	/* initialize window position */

	/* go to byte boundary */
	n = k & 7;
	DUMPBITS(n);

	/* get the length and its complement */
	NEEDBITS(16)
	n = ((unsigned)b & 0xffff);
	DUMPBITS(16)
	NEEDBITS(16)
	if (n != (unsigned)((~b) & 0xffff))
		return 1;	 /* error in compressed data */
	DUMPBITS(16)

	/* read and output the compressed data */
	while (n--) {
		NEEDBITS(8)
				slide[w++] = (Uchar_t)b;
		if (w == WSIZE) {
			flush_output(w);
			if (s_num_files_to_extract == 0)
				return 1;
			w = 0;
		}
		DUMPBITS(8)
	}


	/* restore the globals from the locals */
	wp = w;	/* restore global window pointer */
	s_bit_buffer = b;	/* restore global bit buffer */
	s_bits_in_buffer = k;

	return 0;
}

/* Decompress an inflated type 1 (fixed Huffman codes) block.	We should
 * either replace this with a custom decoder, or at least precompute the
 * Huffman tables.
 */
static int inflate_fixed() {
	int		i;	/* temporary variable */
	huft_t		*tl;	/* literal/length code table */
	huft_t		*td;	/* distance code table */
	int		bl;	/* lookup bits for tl */
	int		bd;	/* lookup bits for td */
	unsigned	l[288];	/* length list for huft_build */


	/* set up literal table */
	for (i = 0; i < 144; i++)
		l[i] = 8;
	for (; i < 256; i++)
		l[i] = 9;
	for (; i < 280; i++)
		l[i] = 7;
	for (; i < 288; i++)	/* make a complete, but wrong code set */
		l[i] = 8;
	bl = 7;
	if ((i = huft_build(l, 288, 257, cplens, cplext, &tl, &bl)) != 0)
		return i;

	/* set up distance table */
	for (i = 0; i < 30; i++)	/* make an incomplete code set */
		l[i] = 5;
	bd = 5;
	if ((i = huft_build(l, 30, 0, cpdist, cpdext, &td, &bd)) > 1) {
		huft_free(tl);

		return i;
	}

	/* decompress until an end-of-block code */
	if (inflate_codes(tl, td, bl, bd))
		return 1;

	/* free the decoding tables, return */
	huft_free(tl);
	huft_free(td);
	return 0;
}



/* decompress an inflated type 2 (dynamic Huffman codes) block. */
static int inflate_dynamic() {
	int		i;	   /* temporary variables */
	unsigned	j;
	unsigned	l;	   /* last length */
	unsigned	m;	   /* mask for bit lengths table */
	unsigned	n;	   /* number of lengths to get */
	huft_t		*tl;	   /* literal/length code table */
	huft_t		*td;	   /* distance code table */
	int		bl;	   /* lookup bits for tl */
	int		bd;	   /* lookup bits for td */
	unsigned	nb;	   /* number of bit length codes */
	unsigned	nl;	   /* number of literal/length codes */
	unsigned	nd;	   /* number of distance codes */
	unsigned	ll[286+30];/* literal/length and distance code lengths */
	register Ulong_t b;	   /* bit buffer */
	register unsigned k;	   /* number of bits in bit buffer */


	/* make local bit buffer */
	b = s_bit_buffer;
	k = s_bits_in_buffer;

	/* read in table lengths */
	NEEDBITS(5)
	nl = 257 + ((unsigned)b & 0x1f);/* number of literal/length codes */
	DUMPBITS(5)
	NEEDBITS(5)
	nd = 1 + ((unsigned)b & 0x1f);	/* number of distance codes */
	DUMPBITS(5)
	NEEDBITS(4)
	nb = 4 + ((unsigned)b & 0xf);	/* number of bit length codes */
	DUMPBITS(4)
	if (nl > 286 || nd > 30)
		return 1;		/* bad lengths */

	/* read in bit-length-code lengths */
	for (j = 0; j < nb; j++) {
		NEEDBITS(3)
				ll[border[j]] = (unsigned)b & 7;
		DUMPBITS(3)
	}
	for (; j < 19; j++)
		ll[border[j]] = 0;

	/* build decoding table for trees--single level, 7 bit lookup */
	bl = 7;
	if ((i = huft_build(ll, 19, 19, NULL, NULL, &tl, &bl)) != 0) {
		if (i == 1)
			huft_free(tl);
		return i;		/* incomplete code set */
	}

	/* read in literal and distance code lengths */
	n = nl + nd;
	m = s_mask_bits[bl];
	i = l = 0;
	while ((unsigned)i < n) {
		NEEDBITS((unsigned)bl)
				j = (td = tl + ((unsigned)b & m))->b;
		DUMPBITS(j)
		j = td->v.n;
		if (j < 16)		/* length of code in bits (0..15) */
			ll[i++] = l = j;/* save last length in l */
		else if (j == 16) {	/* repeat last length 3 to 6 times */
			NEEDBITS(2)
					j = 3 + ((unsigned)b & 3);
			DUMPBITS(2)
			if ((unsigned)i + j > n)
				return 1;
			while (j--)
				ll[i++] = l;
		} else if (j == 17) { /* 3 to 10 zero length codes */
			NEEDBITS(3)
					j = 3 + ((unsigned)b & 7);
			DUMPBITS(3)
			if ((unsigned)i + j > n)
				return 1;
			while (j--)
				ll[i++] = 0;
			l = 0;
		} else { /* j == 18: 11 to 138 zero length codes */
			NEEDBITS(7)
					j = 11 + ((unsigned)b & 0x7f);
			DUMPBITS(7)
			if ((unsigned)i + j > n)
				return 1;
			while (j--)
				ll[i++] = 0;
			l = 0;
		}
	}


	/* free decoding table for trees */
	huft_free(tl);

	/* restore the global bit buffer */
	s_bit_buffer = b;
	s_bits_in_buffer = k;

	/* build the decoding tables for literal/length and distance codes */
	bl = s_lbits;
	if ((i = huft_build(ll, nl, 257, cplens, cplext, &tl, &bl)) != 0) {
		if (i == 1)
			exitGenericError("incomplete literal tree", 0L);
		return i;	/* incomplete code set */
	}
	bd = s_dbits;
	if ((i = huft_build(ll + nl, nd, 0, cpdist, cpdext, &td, &bd)) != 0) {
		if (i == 1)
			exitGenericError("incomplete distance tree", 0L);
		huft_free(tl);
		return i;	/* incomplete code set */
	}

	/* decompress until an end-of-block code */
	if (inflate_codes(tl, td, bl, bd))
		return 1;

	/* free the decoding tables, return */
	huft_free(tl);
	huft_free(td);

	return 0;
}



/* decompress an inflated block */
static int inflate_block(int *e) {
	unsigned		t;	/* block type */
	register Ulong_t	b;	/* bit buffer */
	register unsigned	k;	/* number of bits in bit buffer */

	/* make local bit buffer */
	b = s_bit_buffer;
	k = s_bits_in_buffer;

	/* read in last block bit */
	NEEDBITS(1)
	*e = (int)b & 1;
	DUMPBITS(1)

	/* read in block type */
	NEEDBITS(2)
	t = (unsigned)b & 3;
	DUMPBITS(2)

	/* restore the global bit buffer */
	s_bit_buffer = b;
	s_bits_in_buffer = k;

	/* inflate that block type */
	if (t == 2)
		return inflate_dynamic();
	if (t == 0)
		return inflate_stored();
	if (t == 1)
		return inflate_fixed();

	/* bad block type */
	return 2;
}



/* decompress an inflated entry */
static int myinflate() {
	int	e;	/* last block flag */
	int	r;	/* result code */
	unsigned h;	/* maximum huft_t's malloc'ed */


	/* initialize window, bit buffer */
	wp = 0;
	s_bits_in_buffer = 0;
	s_bit_buffer = 0;

	/* decompress until the last block */
	h = 0;
	do {
		s_hufts = 0;
		if ((r = inflate_block(&e)) != 0)
			return r;
		if (s_hufts > h)
			h = s_hufts;
	} while (!e);

	/* Undo too much lookahead. The next read will be byte aligned so we
	 * can discard unused bits in the last meaningful byte.
	 */
	while (s_bits_in_buffer >= 8) {
		s_bits_in_buffer -= 8;
		/* inptr--;*/
	}

	/* flush out slide */
	flush_output(wp);

	/* return success */
	return 0;
}


/* Process an archive file.  This involves reading the blocks one at a time
 * (decompressing if necessary as it goes along) and passing them to a untar()
 * function.
 */
static void doarchive(char *filename, char *targetname) {
	char	gunzipname[300];
	int	ch, len;

	/* get the archive owner and group */
	struct stat buf;
	if (stat(filename, &buf) != -1) {
		file_uid = buf.st_uid;
		file_gid = buf.st_gid;
	} else {
		file_uid = -1;
		file_gid = -1;
	}
	/* open the archive */
	s_inname = filename;
	s_infp = fopen(filename, "rb");
	if (!s_infp)
		exitGenericError("Could not open archive file %s", filename);

	/* read the first few bytes, so we can determine whether to decompress */
	fread(slide, 1, sizeof(gzhdr_t), s_infp);
	gzhdr_t *gzhdrSlite = (gzhdr_t *)slide;
	if (gzhdrSlite->magic[0] == MAGIC0 && gzhdrSlite->magic[1] == MAGIC1) {
	/* COMPRESSED WITH GZIP */

	/* Check for unsupported compression types */
	if (gzhdrSlite->compression != DEFLATE)
		exitGenericError("Unsupported compression type", 0L);

		/* If original file name present, use it (unless noname) */
		if (!s_ignore_gzip_name && (gzhdrSlite->flags & NAME) != 0) {
			for (len = 0; (ch = getc(s_infp)) != '\0'; len++)
				gunzipname[len] = ch;
			gunzipname[len] = '\0';
		} else if (s_maketar) {
			/* skip the original file name, if any */
			if ((gzhdrSlite->flags & NAME) != 0) {
				while ((ch = getc(s_infp)) != '\0')
					;
			}

			/* we need to make up a name */
			if (targetname)
				strcpy(gunzipname, targetname);
			else {
				strcpy(gunzipname, filename);
				len = strlen(filename);
				if (len > 3 && (!strcmp(filename + len - 3, ".gz") || !strcmp(filename + len - 3, ".GZ")))
					gunzipname[len - 3] = '\0';
				else if (len > 2 && (!strcmp(filename + len - 2, ".z") || !strcmp(filename + len - 2, ".Z")))
					gunzipname[len - 2] = '\0';
				else if (len > 4 && (!strcmp(filename + len - 4, ".tgz") || !strcmp(filename + len - 4, ".TGZ")))
					strcpy(&gunzipname[len - 4], ".tar");
				else
					strcpy(gunzipname, "untar.out");
				strcpy(gunzipname, targetname);
			}
		}

		/* if we're writing the gunzip output, then create the output file */
		if (s_maketar) {
			s_tarfp = fopen(gunzipname, s_convert_newlines ? "w" : "wb");
			if (!s_tarfp)
				exitGenericError("Could not create file %s", gunzipname);
		}

		/* inflate the blocks */
		if (myinflate() != 0 && s_num_files_to_extract != 0)
			exitGenericError("Bad compression data %s", filename);
	} else {
		/* UNCOMPRESSED */
		/* if we were supposed to just decompress, complain */
		if (s_maketar)
			exitGenericError("File %s isn't gzipped", filename);

		/* read the rest of the first block */
		fread(&slide[sizeof(gzhdr_t)], 1, TSIZE - sizeof(gzhdr_t), s_infp);

		/* send each block to the untar() function */
		do {
			untar(slide);
		} while (fread(slide, 1, TSIZE, s_infp) == TSIZE);
	}

	/* close the archive file. */
	fclose(s_infp);
	if (s_tarfp) {
		fclose(s_tarfp);
		s_tarfp = NULL;
		printf("%s: gunzipped to %s\n", filename, gunzipname);
	}
	if (s_out_size > 0) {
		printf("WARNING: Last file might be truncated!\n");
		fclose(s_outfp);
		s_outfp = NULL;
	}
}

/*************************************************************************
 *
 *************************************************************************/
void extractFiles(const char *archiveFile, const char *dir, char **filesToExtract, int silent, char *signatures, char *oldSignatures) {

	s_basedir=(char *)dir;s_is_abs_path=1;s_convert_newlines=0;s_ignore_gzip_name=1;s_maketar=0;quiet=silent;
	s_files_to_extract = filesToExtract;
	s_num_files_to_extract = s_files_to_extract?0:-1;
	s_signatures = signatures;
	s_oldSignatures = oldSignatures;
	if (s_files_to_extract) {
		int i;
		for (i = 0; s_files_to_extract[i]; i ++) {
			char *s;
			for (s = s_files_to_extract[i]; *s != '\0'; s ++)
				if (*s == '\\')
					*s = '/';
			if (s_num_files_to_extract >= 0) {
				if (s_files_to_extract[i][strlen(s_files_to_extract[i])-1] == '/')
					s_num_files_to_extract = -1;
				else
					s_num_files_to_extract ++;
			}
		}
	}
	doarchive((char *)archiveFile, (char *)dir);
}
