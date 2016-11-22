/*************STUDENT***************/
Babak Alipour

Note: NetBeans IDE was used for this project with Scala and Sbt plugins.

/*************ZIP CONTENTS***************/
ScalaProject2 -->
					--->project
					--->src
					--->target
					--->.classpath_nb
					--->README.txt

/*************HOW TO RUN***************/
Change directory to project folder e.g. ./ProjectFB
Run using Sbt build commands:
sbt "run backend" --> will run the program in backend mode
sbt "run frontend" --> will run the program in frontend mode (user simulator)

/*************SOME RESULTS***************/
number of users=10000
average number of requests handled per second=7000
(requests include POST on user, page, post, friendlist and GET on user, page, post, friendlist with a ratio of ~0.01)