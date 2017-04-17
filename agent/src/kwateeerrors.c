/* Copyright (c) 2011, Kwatee Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Kwatee Ltd nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL KWATEE LIMITED,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "kwateeerrors.h"

/*************************************************************************
 *
 *************************************************************************/
void exitWithKwateeCode(const char *kwateeCode, const char *message, const char *param) {
	if (message) {
		char buffer[4000];
		sprintf(buffer, "[kwatee_%s] %s\n", kwateeCode, message);
		printf(buffer, param);
	} else
		printf("[kwatee_%s]\n", kwateeCode);
	exit(0);
}

/*************************************************************************
 *
 *************************************************************************/
void exitNoError(const char *message) {
	exitWithKwateeCode("ok", message, NULL);
}

/*************************************************************************
 *
 *************************************************************************/
void exitGenericError(const char *message, const char *param) {
	exitWithKwateeCode("error", message, param);
}

/*************************************************************************
 *
 *************************************************************************/
void exitNoDescriptorError(const char *message, const char *param) {
	exitWithKwateeCode("no_descriptor_error", message?message:"not deployed", param);
}

/*************************************************************************
 *
 *************************************************************************/
void exitTamperingError(const char *message, const char *param) {
	exitWithKwateeCode("tampering_error", message, param);
}

/*************************************************************************
 *
 *************************************************************************/
void exitOutOfDateError(const char *message, const char *param) {
	exitWithKwateeCode("out_of_date_error", message, param);
}
