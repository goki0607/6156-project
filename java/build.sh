main_file="$1"
rest_file="${@:2}"
mont_file="${main_file%.java}Monitorable"
jar_file=target/monitor-1.0-SNAPSHOT-shaded.jar
if [ ! -f "$jar_file" ]; then
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
