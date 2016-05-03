# Hacker Slacker's Code Challenge Project

##Requirements

This requires java 1.8 or better to be installed.  

For building you will also require maven

Java and Maven executables must be on your PATH for these instructions to work

##Build Instructions

*clone the repository*

git clone https://github.com/pslack/codechallenge.git

*switch to the root directory of this project containing pom.xml*

cd codechallenge

*type the following command to build with maven*

mvn clean install

##Running instructions

*After a successful build the file will be located in the target/ directory*

*To run the file issue the command*

 java -jar target/codechallenge-1.0-SNAPSHOT.jar

the result file will be placed in the current working directory called : codeChallenge.txt

##Documetnation

Javadocs can be found here

http://pjslack.com/codechallenge

###notes
Do not be alarmed of the errors during the test phase of the maven build, these are meant to fail.  This tests whether certain things will fail.

This is a shaded jar meaning that you do not require any other jars to be on the class path in order to execute the JAR file.  The JAR is also an executable jar, one of my favourite things about shaded jars, on OSX or Windows for example, you can simply double click the jar file and it will launch and run as long as java is installed on your machine.  This has been tested on Debian 7.6, both Java 1.8 and Maven are available through aptitude on the Debian platform.


