package client;

import classes.SpaceMarine;
import request.AddCommands;
import request.AddRequest;
import request.Request;
import response.Response;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client {
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    SocketChannel socket;
    String address;
    int port;

    public Client(String address, int port) throws IOException {
        this.address = address;
        this.port = port;
        System.out.println("Здравствуйте, " + System.getProperty("user.name") +"!");
        System.out.println("Введите команду (для получения списка всех команд введите \"help\"): ");
    }
    public void sendCommand() throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String line = input.readLine();


        if (line == null) {
            System.out.println("Вы вышли из приложения");
            System.exit(0);
        }
        else if (line.equals("exit")) {
            System.exit(0);
        }
        else if (!line.isEmpty()) {

            String[] tokens = line.split(" ");
            if (tokens.length == 0) {
                System.out.println("Повторите попытку");
                sendCommand();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Request request = new Request(tokens);

            try (ObjectOutputStream output = new ObjectOutputStream(baos)) {
                if (AddCommands.getNames().contains(line) & tokens.length == 1) {
                    AddRequest addRequest = new AddRequest();
                    SpaceMarine spaceMarine = addRequest.run(new Scanner(System.in));
                    request.addSpaceMarine(spaceMarine);
                }
                output.writeObject(request);
                output.flush();

            }

            //System.out.println("Sending object to server");
            try {
                socket.write(ByteBuffer.wrap(baos.toByteArray()));
            }
            catch (NullPointerException e) {
                connect();
            }
        }

        else {
            System.out.println("Повторите попытку");
            sendCommand();
        }

    }
    public void receiveObject() throws IOException, ClassNotFoundException {
        int bytesRead;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((bytesRead = socket.read(buffer))!=-1){
            buffer.flip();
            byte[] bytes = new byte[bytesRead];
            buffer.get(bytes);
            baos.write(bytes);
            buffer.clear();

        }
        byte[] bytes = baos.toByteArray();
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));){
            Response response = (Response) ois.readObject();
            System.out.println(response.getMessage());


        }

        catch (ClassNotFoundException ignored) {}
        catch (SocketException e) {
            connect();
            receiveObject();
        }
        //System.out.println("Received object from server");
    }
    public void connect()  {
        InetSocketAddress addr = new InetSocketAddress(address, port);
        try {
            socket = SocketChannel.open(addr);
            socket.configureBlocking(false);
        } catch (UnresolvedAddressException e){
            System.err.println("Несуществующий адрес, повторите попытку");
            System.exit(0);
        }
        catch (IOException e) {
            System.out.println("Проблема с подключением");
            System.out.println("Повторить? (y - да), (n - нет)");
            Scanner scanner = new Scanner(System.in);
            String line = "";
            try {
                line = scanner.nextLine();
            }
            catch (NoSuchElementException ignored) {}

            if (line.equals("y")){
                System.out.println("Введите команду");
                this.connect();
            }
            else if (line.equals("n")){
                System.exit(1);
            }
        }
        handleConnection();
    }
    public void handleConnection(){
        try {


            sendCommand();
            receiveObject();

        }
        catch (EOFException e){


        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            connect();

        }
    }
    public void startConnection(){
        while (true){
            this.connect();
        }
    }
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите адрес сервера");
        String address ="";
        int port = 0;
        try {
            address = scanner.nextLine();
            System.out.println("Введите порт");
            port = scanner.nextInt();
        }
        catch (NoSuchElementException e) {
            System.out.println("Вы вышли из приложения");
            System.exit(0);
        }

        Client client = new Client(address, port);


        client.startConnection();

    }
}
