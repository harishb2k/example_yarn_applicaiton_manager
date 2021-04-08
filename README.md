### Launch your app on Yarn
This is how Spark Submit works. Sometime you way want to run your work on YARN cluster.

##### Example
Suppose you have the following code which you want to run on YARN. Make a jar with all dependency (Fat Jar).
```java
public class App {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 50; i++) {
            Thread.sleep(1000);
            System.out.println("Running app : " + i);
        }
    }
}
```

#### How to do it
Update the example code with your raj
```java
update org.example.App
>> Path jarPath = new Path("file://<Your Jar file>");
```

#### Finally, Run it
```shell
mvn clean install
java -jar target/yarn_applicaiton_manager-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

Now you can see this job will be running on YARN cluster