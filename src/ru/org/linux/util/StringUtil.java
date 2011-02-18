/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.util;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class StringUtil {
  private static final Random random = new Random();

  private StringUtil() {
  }

  public static String getFileName(String pathname) {
    StringTokenizer parsed = new StringTokenizer(pathname, "/\\", false);
    String filename = "";
    while (parsed.hasMoreElements()) {
      filename = parsed.nextToken();
    }
    return filename;
  }

  private static final Pattern loginCheckRE = Pattern.compile("[a-z][a-z0-9_-]*");

  public static boolean checkLoginName(String login) {
    login = login.toLowerCase();

    // no zerosize login
    if (login.length() == 0) {
      return false;
    }
    if (login.length() >= 80) {
      return false;
    }

    return loginCheckRE.matcher(login).matches();
  }

  public static String makeTitle(String title) {
    if (title != null && !"".equals(title.trim())) {
      return title;
    }
    return "Без заглавия";
  }

  public static String md5hash(String pass) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
      BigInteger bi = new BigInteger(1, md.digest(pass.getBytes()));
      String hash = bi.toString(16);
      if (hash.length() < 32) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 32 - hash.length(); i++) {
          buf.append('0');
        }
        buf.append(hash);
//        logger.fine("Calculated hash="+buf.toString()); //$NON-NLS-1$
        return buf.toString();
      } else {
        return hash;
      }
    } catch (GeneralSecurityException gse) {
      throw new RuntimeException(gse);
    }
  }

  public static String generatePassword() {
    StringBuilder builder = new StringBuilder();

    for (int i=0; i<10; i++) {
      int r = Math.abs(random.nextInt());

      builder.append((char) (33 + r%(126-33)));
    }

    return builder.toString();
  }
}