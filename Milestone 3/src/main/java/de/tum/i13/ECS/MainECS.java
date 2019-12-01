package de.tum.i13.ECS;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class MainECS {
    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("ECS: Specify the port and the log level");
            System.out.println("Example: java -jar target/ECS.jar 5134 ALL");
            System.exit(-1);
        }
        else{
            Thread ecs = new Thread(new ECS(Integer.parseInt(args[0]), args[1]));
            ecs.start();
        }

    }
}
