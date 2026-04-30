import org.springframework.security.crypto.bcrypt.BCrypt;
public class BcryptTool {
  public static void main(String[] args) {
    if (args[0].equals("hash")) {
      System.out.print(BCrypt.hashpw(args[1], BCrypt.gensalt(12)));
    } else if (args[0].equals("check")) {
      System.out.print(BCrypt.checkpw(args[1], args[2]) ? "MATCH" : "NO_MATCH");
    }
  }
}
