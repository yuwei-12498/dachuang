import org.springframework.security.crypto.bcrypt.BCrypt;
public class CheckBcrypt {
  public static void main(String[] args) {
    System.out.print(BCrypt.checkpw(args[0], args[1]) ? "MATCH" : "NO_MATCH");
  }
}
