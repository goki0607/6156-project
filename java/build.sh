main_file="$1"
rest_file="${@:2}"
mont_file="${main_file%.java}Monitorable"
if [ ! -f /target/monitor-1.0-SNAPSHOT-shaded.jar ]; then
  mvn clean
  mvn clean install
fi
java -jar target/monitor-1.0-SNAPSHOT-shaded.jar $main_file $rest_file
cd out
mkdir "classes"
javac *.java -d classes
cd "classes"
java $mont_file
cd ..
rm -r "classes"
rm "${mont_file}.java"
cd ..
#exec_file="classes/${mont_file}.class"
#mv $exec_file "../"
#rm -r "classes"
#cd out
#mkdir classes
#javac *.java -d out/classes
#mv classes/mont_file ../
#cd ..
