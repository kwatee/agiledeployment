GCC:=gcc 
LINK:=gcc
STRIP:=strip

AGENT_VERSION=`cat ../platforms.properties | grep -e 'version=.*' | sed 's/version=//'`
KWAGENT=kwagent
AGENT_C_SRCS = ../src/descriptor.c ../src/ezxml.c ../src/hashmap.c ../src/utils.c ../src/main_agent.c ../src/md5.c ../src/kwateeerrors.c ../src/untar.c ../src/kwateevars.c
AGENT_OBJS = descriptor.o ezxml.o hashmap.o utils.o main_agent.o md5.o kwateeerrors.o untar.o kwateevars.o
AGENT_C_DEPS = descriptor.d ezxml.d hashmap.d utils.d main_agent.d md5.d kwateeerrors.d untar.d kwateevars.d

ifneq ($(MAKECMDGOALS),clean)
-include $(AGENT_C_DEPS)
endif

%.o: ../src/%.c
	@echo 'Compiling: $<'
	@$(GCC) -DAGENT_VERSION=\"$(AGENT_VERSION)\" -DPOSIX -O3 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"

$(KWAGENT): $(AGENT_OBJS)
	@$(LINK) -o "$(KWAGENT)" $(AGENT_OBJS)
	@$(STRIP) $(KWAGENT)
	@echo 'Done building $(KWAGENT)'

all: $(KWAGENT)

clean:
	@rm -f $(AGENT_OBJS) $(AGENT_C_DEPS) $(KWAGENT)
