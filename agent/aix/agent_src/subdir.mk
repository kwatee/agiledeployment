# Add inputs and outputs from these tool invocations to the build variables 
AGENT_C_SRCS += \
../src/descriptor.c \
../src/ezxml.c \
../src/hashmap.c \
../src/utils.c \
../src/main_agent.c \
../src/md5.c \
../src/kwateeerrors.c \
../src/untar.c

AGENT_OBJS += \
./agent_src/descriptor.o \
./agent_src/ezxml.o \
./agent_src/hashmap.o \
./agent_src/utils.o \
./agent_src/main_agent.o \
./agent_src/md5.o \
./agent_src/kwateeerrors.o \
./agent_src/untar.o

AGENT_C_DEPS += \
./agent_src/descriptor.d \
./agent_src/ezxml.d \
./agent_src/hashmap.d \
./agent_src/utils.d \
./agent_src/main_agent.d \
./agent_src/md5.d \
./agent_src/kwateeerrors.d \
./agent_src/untar.d

# Each subdirectory must supply rules for building sources it contributes
agent_src/%.o: ../src/%.c
	@echo 'Compiling: $<'
	@$(GCC) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
