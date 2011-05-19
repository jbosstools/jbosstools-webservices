This test plugin may require additional setup to successfully build.

In Hudson we do this:

echo "${NATIVE_TOOLS}${SEP}${JAVA15}"
export PATH=${NATIVE_TOOLS}${SEP}${JAVA15}/bin:$PATH
java -version

then pass in these JVM options to maven:

	-Xms512m -Xmx1024m -XX:PermSize=128m -XX:MaxPermSize=256m \
	-Djbosstools.test.jre.5="${NATIVE_TOOLS}${SEP}${JAVA15}" \
	-Djbosstools.test.jre.6="${NATIVE_TOOLS}${SEP}${JAVA16}"

where ${NATIVE_TOOLS}${SEP}${JAVA16} and ${NATIVE_TOOLS}${SEP}${JAVA15} may be something like:

/opt/sun-java2-5.0/
/opt/sun-java2-6.0/

