package work.pomz;

import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Main {
    private static Web3j web3j;
    private static Credentials credentials;
    private static String url = "http://localhost:8545";
    private static String privateKeyRaw = null;
    private static BigInteger privateKey = null;
    private static final String tokenAddress = <YOUR TOKWN ADDRESS>;
    private static BigInteger gasPrice = BigInteger.valueOf(17000000000l);
    private static BigInteger gasLimit = BigInteger.valueOf(60000);
    private static TPOMZ tokenTPOMZ;

    public static void main(String[] args) {
        web3j = Web3j.build(new HttpService(url));

        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        credentials = Credentials.create(ecKeyPair);

        Long summ = 0l;
        String tx = null;

        tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(8189);
                 Socket socket = serverSocket.accept()) {
                System.out.println("Client connected");
                OutputStream os = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(os, true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = br.readLine();
                String response = null;
                String request[] = msg.split("\\s");
                if (request[0].equalsIgnoreCase("setgaslimit")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    if (request[1].matches("\\D")) {
                        response = "Incorrect argument, expected long value";
                        break;
                    }
                    Long gl = new Long(request[1]);
                    gasLimit = BigInteger.valueOf(gl);
                    tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
                    response = "Success: Gas Limit is " + gl;
                } else if (request[0].equalsIgnoreCase("setgasprice")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    if (request[1].matches("\\D")) {
                        response = "Incorrect argument, expected integer value";
                        break;
                    }
                    Long gp = new Long(request[1]);
                    gasPrice = BigInteger.valueOf(gp);
                    tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
                    response = "Success: Gas Price is " + gp;
                } else if (request[0].equalsIgnoreCase("setnode")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    url = request[1];
                    web3j = Web3j.build(new HttpService(url));
                    tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
                    response = "Success: url node is " + url;
                } else if (request[0].equalsIgnoreCase("setprivatekey")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    privateKeyRaw = request[1];
                    privateKey = new BigInteger(privateKeyRaw, 16);
                    credentials = Credentials.create(ecKeyPair);
                    tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
                } else if (request[0].equalsIgnoreCase("getgasprice")) {
                    response = String.valueOf(gasPrice);
                } else if (request[0].equalsIgnoreCase("getgaslimit")) {
                    response = String.valueOf(gasLimit);
                } else if (request[0].equalsIgnoreCase("getaccount")) {
                    response = credentials.getAddress();
                } else if (request[0].equalsIgnoreCase("setnode")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    url = request[1];
                    web3j = Web3j.build(new HttpService(url));
                    tokenTPOMZ = TPOMZ.load(tokenAddress, web3j, credentials, gasPrice, gasLimit);
                } else if (request[0].equalsIgnoreCase("getnode")) {
                    response = url;
                } else if (request[0].equalsIgnoreCase("newwallet")) {
                    if (request.length == 1) {
                        try {
                            response = createWallet();
                        } catch (InvalidAlgorithmParameterException e) {
                            response = e.getMessage();
                        } catch (NoSuchAlgorithmException e) {
                            response = e.getMessage();
                        } catch (NoSuchProviderException e) {
                            response = e.getMessage();
                        }
                    } else {
                        try {
                            response = createWallet(request[1]);
                        } catch (InvalidAlgorithmParameterException e) {
                            response = e.getMessage();
                        } catch (NoSuchAlgorithmException e) {
                            response = e.getMessage();
                        } catch (NoSuchProviderException e) {
                            response = e.getMessage();
                        } catch (CipherException e) {
                            response = e.getMessage();
                        }
                    }
                } else if (request[0].equalsIgnoreCase("balanceof")) {
                    if (request.length < 2) {
                        response = "Missed argument";
                        break;
                    }
                    try {
                        response = getTpomzBalance(request[1]).toEngineeringString();
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                } else if (request[0].equalsIgnoreCase("transfer")) {
                    if (request.length < 3) {
                        response = "Missed argument";
                        break;
                    }
                    Long count = Long.valueOf(request[2]);
                    try {
                        response = sendTPOMZ(request[1], count);
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                } else if (request[0].equalsIgnoreCase("help")) {
                    response = "setGasLimit\n\tSet gas limit for transaction\n\tExample: setGasLimit <integer gas limit>\n\tDefault: 60000\n\n" +
                            "setGasPrice\n\tSet gas price in Wei\n\tExample: setGasPrice <long gas price>\n\tDefault: 17000000000\n\n" +
                            "setNode\n\tSet node url\n\tExample: setNode <string url>\n\tDefault: http://localhost:8545\n\n" +
                            "setPrivateKey\n\tSet private key for wallet\n\tExample: setPrivateKey <string private key>\n\n" +
                            "setWalletFile\n\tSet wallet file and password\n\tExample: setWalletFile <string password>\n\n" +
                            "getGasPrice\n\tGet current gas price settings\n\n" +
                            "getGasLimit\n\tGet current gas limit settings\n\n" +
                            "getAccount\n\tGet current account\n\n" +
                            "getNode\n\tGet current node url\n\n" +
                            "balanceOf\n\tGet balance of account in TPOMZ tokens\n\tExample: balanceOf <string account>\n\n" +
                            "transfer\n\tTransfer TPOMZ tokens from current account to another\n\t" +
                            "Example: transfer <string recipient account> <long count>\n\t" +
                            "Decimals = 8 i.e. 100000000 = 1 TPOMZ or 1 = 0.00000001 TPOMZ\n\n" +
                            "newWallet\n\tCreate new wallet and return keys\n\t" +
                            "Example: newWallet <string password> — create wallet file encrypted by password\n\t" +
                            "Example: newWallet — create private key\n\n" +
                            "help\n\tShow quick help on commands";
                } else {
                    response = "unknown command";
                }
                pw.println(response);
                pw.close();
                br.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String sendTPOMZ(String to, Long value) throws Exception {
        RemoteCall<TransactionReceipt> transactionReceiptRemoteCall =
                tokenTPOMZ.transfer(to, BigInteger.valueOf(value));
        TransactionReceipt transactionReceipt = transactionReceiptRemoteCall.send();

        return transactionReceipt.getTransactionHash();
    }

    private static String createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        Credentials cr = Credentials.create(ecKeyPair);
        return cr.getAddress() + " " + cr.getEcKeyPair().getPrivateKey().toString(16);
    }

    private static String createWallet(String password) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CipherException, IOException {
        String walletFile = WalletUtils.generateFullNewWalletFile(password, new File(WalletUtils.getDefaultKeyDirectory()));
        return walletFile;
    }

    private static BigDecimal getTpomzBalance(String address) throws Exception {
        RemoteCall<BigInteger> bigIntegerRemoteCall = tokenTPOMZ.balanceOf(address);
        BigInteger bigInteger = bigIntegerRemoteCall.send();
        BigDecimal bigDecimal = new BigDecimal(bigInteger);
        return bigDecimal.divide(BigDecimal.valueOf(Math.pow(10, 8)));
    }
}
