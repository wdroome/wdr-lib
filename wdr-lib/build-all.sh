#!/bin/sh

##
## The jar to create with the WDR-LIB classes.
## NOTE: This must be an absolute path name.
##
LIB_JAR="$(pwd)/wdr-lib.jar"

##
## A blank-sep list of jar files (or jar directories) for external packages
## needed to compile this library. These will go into CLASSPATH.
## If the list includes a directory, we will add every jar file
## under that directory to CLASSPATH.
## If there are no external jars, set this to an empty directory (or /dev/null).
##
JAR_DIRS="/dev/null"

##
## The directory for the compiled .class files.
## NOTE: We will create this directory if needed,
## and we will overwrite old .class files for existing classes,
## but we will NOT remove old .class files for deleted classes.
##
CLASS_DIR="classes"

mkdir -p ${CLASS_DIR}

javac -sourcepath src \
		-d ${CLASS_DIR} \
		-cp $(find ${JAR_DIRS} -name '*.jar' | tr '\n' ':') \
		$(find src/com/wdroome -name '*.java')
		
rm -f ${LIB_JAR}
cd ${CLASS_DIR}
jar cf ${LIB_JAR} $(find . -name '*.class')
