/*
 * This source code is protected by the EUPL version 1.2 and is part of the "PP Decrypt" example package.
 * 
 * Copyright: Logius (2018)
 * @author: Bram van Pelt 
 */
package nl.logius.resource.pp.example;

import java.io.File;
import java.lang.reflect.*;
import java.security.Security;
import java.util.Map;

import javax.crypto.Cipher;

import nl.logius.resource.pp.util.DecryptUtil;
import org.apache.commons.io.FileUtils;

public class Main {

  public static void main(String[] args) {
    try {
      // In dit voorbeeld werken we me langere sleutels dan normaal wordt gebruikt in Java. 
      // Hiervoor moet een aanpassing tijdelijk gemaakt worden aan de java security opties. Dit gebeurd in de bijgevoegde methode
      // Normaal kan hiervoor een patch van Oracle worden geinstalleerd. 
      // voor java 1.8 is dat http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
      fixKeyLength();

      // ook bouncycastle moet worden geregistreerd als JAVA security provider. Normaal wordt dit op de server gedaan voordat de software wordt gerunned.
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

      // laad de sleutels in het geheugen, normaal komen deze uit een keystore
      KeyUtil keys = new KeyUtil();

      //simuleer een EncryptedID (ei) en EncryptedPseudonym (ep)
      File eiFile = new File(Main.class.getClassLoader().getResource("signed/900095222-2-4-I.txt").getFile());
      String ei = FileUtils.readFileToString(eiFile, "UTF-8");
      File epFile = new File(Main.class.getClassLoader().getResource("signed/900095222-2-4-P.txt").getFile());
      String ep = FileUtils.readFileToString(epFile, "UTF-8");

      //Pre-load complete, Decrypt de ei en ep
      String simBsn = DecryptUtil.getIdentity(ei, keys.getDecryptKey(), keys.getVerifiers());
      String simPseudo = DecryptUtil.getPseudonym(ep, keys.getPDecryptKey(), keys.getPClosingKey(), keys.getPVerifiers());

      //Doe er iets nuttigs mee ;)
      System.out.println("Identity:" + simBsn);
      System.out.println("Pseudo:" + simPseudo);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void fixKeyLength() {
    final String ERRORMESSAGE = "Failed manually overriding key-length permissions.";
    int newMaxKeyLength;
    try {
      if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
        Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
        Constructor con = c.getDeclaredConstructor();
        con.setAccessible(true);
        Object allPermissionCollection = con.newInstance();
        Field f = c.getDeclaredField("all_allowed");
        f.setAccessible(true);
        f.setBoolean(allPermissionCollection, true);

        c = Class.forName("javax.crypto.CryptoPermissions");
        con = c.getDeclaredConstructor();
        con.setAccessible(true);
        Object allPermissions = con.newInstance();
        f = c.getDeclaredField("perms");
        f.setAccessible(true);
        ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

        c = Class.forName("javax.crypto.JceSecurityManager");
        f = c.getDeclaredField("defaultPolicy");
        f.setAccessible(true);
        Field mf = Field.class.getDeclaredField("modifiers");
        mf.setAccessible(true);
        mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, allPermissions);

        newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
      }
    } catch (Exception e) {
      throw new RuntimeException(ERRORMESSAGE, e);
    }
    if (newMaxKeyLength < 256) {
      throw new RuntimeException(ERRORMESSAGE); // hack failed
    }
  }

}
