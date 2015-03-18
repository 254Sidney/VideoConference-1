package wideokomunikator.server;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;


/**
 *
 * @author Piotr
 */
public class PasswordSecurity {
    private static final int SALT_SIZE = 64;
    private static final String ALGORITHM = "SHA-512";
    public static final int HASH_SIZE = 128;
        
    
    public static byte[] getHash(String password) throws NoSuchAlgorithmException{
        return getHash(password.toCharArray());
    } 
    public static byte[] getHash(char[] password) throws NoSuchAlgorithmException{
        byte salt[] = new byte[SALT_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        byte[] hash = sha512(password, salt);
        byte[] result = new byte[SALT_SIZE+MessageDigest.getInstance(ALGORITHM).getDigestLength()];
        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(hash, 0, result, salt.length, hash.length);
        return result;
    }
    
    private static byte[] sha512(char[] password,byte[] salt) throws NoSuchAlgorithmException{      
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        digest.update(salt);
        byte[] hash;
        hash = digest.digest(new String(password).getBytes());
        digest.reset();
        return  hash;
    }
    
    public static boolean validatePassword(char[] password,byte[] hash_bytes) throws NoSuchAlgorithmException{
        try{
            
            byte[] salt = Arrays.copyOfRange(hash_bytes, 0, SALT_SIZE);
            byte[] hash = Arrays.copyOfRange(hash_bytes, SALT_SIZE, hash_bytes.length);
            byte[] newHash = sha512(password, salt);
            boolean result = equal(newHash, hash);
            return result;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    private static boolean equal(byte[] hashA,byte[] hashB){        
        if(hashA.length!=hashB.length){
            return false;
        }
        for(int i=0;i<hashA.length&&i<hashB.length;i++){
            if(hashA[i]!=hashB[i])
                return false;
        }  
        return true;
    }
    public static boolean validatePassword(String password,byte[] hash) throws NoSuchAlgorithmException{        
        return  validatePassword(password.toCharArray(), hash);        
    }
    
    public static String byteToHex(byte[] bytes){
        String text="";
        for(byte b:bytes){
            text+=byteToHex(b);
        }
        return text;
    }
    public static String byteToHex(byte b) {
      // Returns hex String representation of byte b
      char hexDigit[] = {
         '0', '1', '2', '3', '4', '5', '6', '7',
         '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
      };
      char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
      return new String(array);
   }
 
   public static String charToHex(char c) {
      // Returns hex String representation of char c
      byte hi = (byte) (c >>> 8);
      byte lo = (byte) (c & 0xff);
      return byteToHex(hi) + byteToHex(lo);
   }
}
