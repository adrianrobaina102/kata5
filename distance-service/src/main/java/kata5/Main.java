package kata5;

public class Main {
    static void main(String[] args) {
        WebService service = new WebService(args[0], args[1], Integer.parseInt(args[2]), args[3]);
        service.start();
    }
}
