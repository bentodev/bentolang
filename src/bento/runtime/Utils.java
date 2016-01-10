/* Bento
 *
 * $Id: Utils.java,v 1.48 2015/04/20 12:50:41 sthippo Exp $
 *
 * Copyright (c) 2002-2016 by bentodev.org
 *
 * Use of this code in source or compiled form is subject to the
 * Bento Poetic License at http://www.bentodev.org/poetic-license.html
 */

package bento.runtime;

import bento.lang.Redirection;

import java.util.regex.Pattern;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Runtime utility methods accessible to Bento code.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.48 $
 */

public class Utils {
	
    
    /** Causes the current thread (i.e., the current instantiation) to sleep.  Should
     *  only be called from a concurrent instantiation. 
     */
    public static void sleep(long millis) throws Redirection {
        try {
            Thread.sleep(millis);
            
        } catch (InterruptedException ie) {
            String errmsg = "sleep(" + millis + ") interrupted";
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        }
    }
    
	public static String chr$(int n) {
	    return Character.toString((char) n);	
	}
	
	public static String hex(int n) {
	    return Integer.toHexString(n);
	}

    public static Map<String, Object> newSortedTable() {
        return new TreeMap<String, Object>();
    }
    
    public static Map<String, Object> sortedTable(Map<String, Object> map) {
        if (map != null) {
            return new TreeMap<String, Object>(map);
        } else {
            return new TreeMap<String, Object>();
        }
    }
    
    public static List<Object> sortedArray(List<Object> list) {
        return Arrays.asList(sortedArray(list.toArray()));
    }

    public static Object[] sortedArray(Object[] array) {
        if (array != null && array.length > 0) {
            Object[] sorted = new Object[array.length];
            System.arraycopy(array, 0, sorted, 0, array.length);
            Arrays.sort(sorted);
            return sorted;
        } else {
            return new Object[0];
        }
    }
    
    public static int strlen(String str) {
    	if (str != null) {
    		return str.length();
        } else {
        	return 0;
        }
    }

    public static String toLower(String str) {
        if (str != null) {
            return str.toLowerCase();
        } else {
            return null;
        }
    }
    
    public static String toUpper(String str) {
        if (str != null) {
            return str.toUpperCase();
        } else {
            return null;
        }
    }
   
    public static boolean startsWith(String str, String substr) {
        if (str != null) {
            return str.startsWith(substr);
        } else {
            return false;
        }
    }

    public static boolean endsWith(String str, String substr) {
        if (str != null) {
            return str.endsWith(substr);
        } else {
            return false;
        }
    }

    public static int indexOf(String str, String substr) {
        if (str != null) {
            return str.indexOf(substr);
        } else {
            return -1;
        }
    }

    public static int indexOf(String str, String substr, int ix) {
        if (str != null) {
            return str.indexOf(substr, ix);
        } else {
            return -1;
        }
    }

    public static int lastIndexOf(String str, String substr) {
        if (str != null) {
            return str.lastIndexOf(substr);
        } else {
            return -1;
        }
    }

    public static int lastIndexOf(String str, String substr, int ix) {
        if (str != null) {
            return str.lastIndexOf(substr, ix);
        } else {
            return -1;
        }
    }

    public static String substring(String str, int startIx) {
        return str.substring(startIx);
    }
    
    public static String substring(String str, int startIx, int endIx) {
        return str.substring(startIx, endIx);
    }
    
    public static String trim(String str) {
        if (str != null) {
            return str.trim();
        } else {
            return null;
        }
    }
    
    public static String trim(String str, char c) {
        if (str != null) {
            if (str.length() > 0) {
                int start = 0;
                int end = str.length() - 1;
                while (start <= end && str.charAt(start) == c) {
                    start++;
                }
                while (end > start && str.charAt(end) == c) {
                    end--;
                }
                return str.substring(start, end + 1);
            }
        }
        return str;
    }
            
    public static String trimLeading(String str, char c) {
        if (str != null) {
            if (str.length() > 0) {
                int start = 0;
                while (start < str.length() && str.charAt(start) == c) {
                    start++;
                }
                if (start > 0) {
                    return str.substring(start);
                }
            }
        }
        return str;
    }

    public static String trimLeading(String str, String substr) {
        if (str != null && str.startsWith(substr)) {
            return str.substring(substr.length());
        } else {
            return str;
        }
    }
    
    /** Remove leading white space from a string **/
    public static String trimLeading(String str) {
        int len = (str == null ? 0 : str.length());
        int ix = 0;
        while (ix < len) {
            char c = str.charAt(ix);
            if (c > ' ') {
                break;
            }
            ix++;
        }
        
        if (ix == len) {
            return "";

        } else if (ix > 0) {
            return str.substring(ix);

        } else {
            return str;
        }
    }

    public static String trimTrailing(String str, char c) {
        if (str != null) {
            if (str.length() > 0) {
                int end = str.length() - 1;
                while (end >= 0 && str.charAt(end) == c) {
                    end--;
                }
                if (end < str.length() - 1) {
                    return str.substring(0, end + 1);
                }
            }
        }
        return str;
    }

    /** Remove a trailing substring from a string **/
    public static String trimTrailing(String str, String substr) {
        if (str != null && str.endsWith(substr)) {
            return str.substring(0, str.length() - substr.length());
        } else {
            return str;
        }
    }
    
    /** Remove trailing white space from a string **/
    public static String trimTrailing(String str) {
        int len = str.length();
        int ix = len - 1;
        while (ix >= 0) {
            char c = str.charAt(ix);
            if (c > ' ') {
                break;
            }
            ix--;
        }
        
        if (ix < 0) {
            return "";

        } else if (ix < len - 1) {
            return str.substring(0, ix + 1);

        } else {
            return str;
        }
    }
    

    public static String trimThroughFirst(String str, char c) {
        if (str == null) {
            return null;
        }
        int ix = str.indexOf(c);
        if (ix >= 0) {
            return str.substring(ix + 1);
        } else {
        	return null;
        }
    }

    public static String trimFromLast(String str, char c) {
        if (str == null) {
            return null;
        }
        int ix = str.lastIndexOf(c);
        if (ix >= 0) {
            return str.substring(0, ix + 1);
        } else {
            return str;
        }
    }
    
    
    public static char charAt(String str, int index) {
        if (str != null && str.length() > index) {
            return str.charAt(index);
        } else {
            return '\0';
        }
    }

    public static String insertAt(String str, int index, String substr) {
        if (str != null && str.length() > index) {
           return str.substring(0, index) + substr + str.substring(index);        
        } else {
            return str;
        }
    }
    
    public static String includeFile(String filename) throws Redirection {
        String text = null;
        try {
            File file = new File(filename);
            if (file.exists() && !file.isDirectory()) {
                FileReader in = new FileReader(file);
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[1024];
                int n = 0;
                while ((n = in.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
                in.close();
                text = sb.toString();
            } else {
                SiteBuilder.log("File " + filename + " not found.");
            }
            
        } catch (Exception e) {
            String errmsg = "Exception including " + filename + ": " + e.toString();
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        }

        return text;
    }


    public static String includeURL(String urlname) throws Redirection {
        String text = null;
        try {
            URL url = new URL(urlname);
            Object content = url.getContent();
            if (content instanceof InputStream) {
                byte[] buf = new byte[512];
                InputStream in = (InputStream) content;
                StringBuilder sb = new StringBuilder();
                try {
                    while (true) {
                        int len = in.read(buf, 0, 512);
                        if (len <= 0) {
                            break;
                        }
                        sb.append(new String(buf, 0, len));
                    }
                } finally {
                    in.close();
                }
                text = sb.toString();

            } else {
                text = content.toString();
            }

        } catch (MalformedURLException mue) {
            String errmsg = "Malformed URL in include call: " + urlname;
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        } catch (Exception e) {
            String errmsg = "Exception including " + urlname + ": " + e.toString();
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        }

        return text;
    }

    public static String encodeURL(String urlname) throws Redirection {
        String text = null;
        try {
            URL url = new URL(urlname);
            Object content = url.getContent();
            if (content instanceof InputStream) {
                InputStream in = (InputStream) content;
                StringBuilder sb = new StringBuilder();
                try {
                    while (true) {
                        int c = in.read();
                        if (c < 0) {
                            break;
                        } else if (c == '<') {
                            sb.append("&lt;");
                        } else {
                            sb.append((char) c);
                        }
                    }
                } finally {
                    in.close();
                }
                text = sb.toString();

            } else {
                text = content.toString();
            }

        } catch (MalformedURLException mue) {
            String errmsg = "Malformed URL in include call: " + urlname;
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        } catch (Exception e) {
            String errmsg = "Exception including " + urlname + ": " + e.toString();
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        }

        return text;
    }

    public static String encode(String str) {
        return htmlEncode(str);
    }
    
    public static String htmlEncode(String str) {
        if (str != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '<') {
                    sb.append("&lt;");
                } else {
                	sb.append(c);
                }
            }   
            return sb.toString();

        } else {
            return null;
        }
    }
    
    public static String urlEncode(String str) throws Redirection {
        if (str != null) {
            try {
                return URLEncoder.encode(str, "UTF-8");

            } catch (UnsupportedEncodingException e) {
                String errmsg = "unable to url encode " + str;
                SiteBuilder.log(errmsg);
                throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
            }

        } else {
            return null;
        }
    }
    public static String replaceAllOccurrences(String str, Map<String, String> replacementMap) {
    	Set<Map.Entry<String, String>> entries = replacementMap.entrySet();
    	Iterator<Map.Entry<String, String>> it = entries.iterator();
    	while (it.hasNext()) {
        	Map.Entry<String, String> entry = it.next();
        	str = replaceOccurrences(str, entry.getKey(), entry.getValue());
    	}
    	return str;
    }
    

    
    public static String replaceOccurrences(String str, String oldField, String newField) {
        if (str == null) {
            return null;
        }
        int ix = str.indexOf(oldField);
        if (ix < 0) {
        	return str;
        }

        StringBuilder sb = new StringBuilder();
        int len = oldField.length();
        
        while (ix >= 0) {
            if (ix > 0) {
                sb.append(str.substring(0, ix));
            }
            sb.append(newField);
            str = str.substring(ix + len);
            ix = str.indexOf(oldField);
        }
        if (str.length() > 0) {
            sb.append(str);
        }
        return sb.toString();
    }



    /** Returns the first HTML paragraph in the passed string.  A paragraph is defined as
     *  text delimited by an HTML paragraph tag, which includes <p>, <h1> to <h6>, <li>,
     *  <blockquote> and <td>.  If the first non-whitespace text in the string is a paragraph
     *  tag, the first paragraph consists of all the text up to but not including the second
     *  paragraph tag (or the entire string if no additional paragraph tag is found).  But if
     *  the string begins with something other than a paragraph tag, the first paragraph
     *  consists of all the text up to but not including the firstparagraph tag (or the entire
     *  string if no paragraph tag is found).
     *
     *  The minlength and maxlength parameters, if greater than zero, set minimum and
     *  maximum bounds on the non-whitespace, non-tag length of the string returned.  In
     *  this case paragraph tags before minlength are ignored; also, if no paragraph tag
     *  is found before maxlength, the string is terminated at the whitespace preceding
     *  maxlength (or at maxlength if the string contains no prior whitespace).
     */
    public static String getFirstHTMLParagraph(String str, int minlength, int maxlength) {
        int len = str.length();
        String firstPara = str;
        int whitespaceIndex = 0;
        int lastNonwhite = 0;
        int numNonwhite = 0;

        int beginTag = 0;
        boolean openTag = false;
        boolean beginHeader = false;
        boolean afterName = false;
        boolean closeTag = false;
        boolean leadText = false;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (whitespaceIndex < lastNonwhite) {
                    whitespaceIndex = i;
                }
                continue;
            } else {
                lastNonwhite = i;
            }

            if (closeTag) {

                if (c == '>') {
                    if (leadText && (minlength < 1 || minlength <= numNonwhite)) {
                        firstPara = str.substring(0, beginTag);
                        break;
                    } else {
                        leadText = true;
                        closeTag = false;
                    }
                }

            } else if (beginHeader) {
                if (c >= '1' && c <= '6') {
                    closeTag = true;
                }
                beginHeader = false;


            } else if (afterName) {
                if (Character.isWhitespace(c) || c == '/') {
                    afterName = false;
                    closeTag = true;
                } else if (c == '>') {
                    afterName = false;
                    closeTag = true;
                    // back up to reprocess c
                    i--;
                // a less than sign can only mean we're not in a tag after all
                } else if (c == '<') {
                    afterName = false;
                    // back up to reprocess c
                    i--;
                }

            } else if (openTag) {
                if (i < len - 1) {
                    char nextc = str.charAt(i + 1);
                    switch (c) {
                        case 'p':
                        case 'P':
                            afterName = true;
                            break;
                        case 'h':
                        case 'H':
                            if (nextc >= '1' && nextc <= '6') {
                                beginHeader = true;
                            }
                            break;
                        case 'l':
                        case 'L':
                            if (nextc == 'i' || nextc == 'I') {
                                afterName = true;
                                i++;
                            }
                            break;

                        case 't':
                        case 'T':
                            if (nextc == 'd' || nextc == 'D') {
                                afterName = true;
                                i++;
                            }
                            break;
                        case 'b':
                        case 'B':
                            if (i < len - 10 && str.substring(i + 1, i + 10).equalsIgnoreCase("lockquote")) {
                                afterName = true;
                                i += 9;
                            }
                            break;
                    }
                }
                openTag = false;

            } else {
                if (c == '<') {
                    openTag = true;
                    beginTag = i;
                } else {
                    leadText = true;
                    // put the check for maximum length here (i.e., at the first nonwhite
                    // character past maxlength) so that tags that come after the final
                    // character are preserved.
                    if (maxlength > 0 && maxlength <= numNonwhite) {
                        firstPara = str.substring(0, i);
                        break;
                    }
                    numNonwhite++;
                }
            }
        }
        return firstPara;
    }

    public static List<String> tokenize(String str, String delims, boolean returnDelims) {
        List<String> list = new ArrayList<String>();
        if (str != null && str.length() > 0) {
            StringTokenizer tok = (delims == null ? new StringTokenizer(str) : new StringTokenizer(str, delims, returnDelims));
            while (tok.hasMoreTokens()) {
                list.add(tok.nextToken());
            }
        }
        return list; 
    }
    
    /** Split a string into a list of lines.  Handles lines terminated
     *  by \n or \r\n; can also handle strings that terminate lines with 
     *  standalone \r characters exclusively (i.e. \n does not appear).
     * @param str
     * @return
     */

    public static String[] split(String str, String regex) {
        if (str == null || str.length() == 0) {
            return new String[0];
        } else {
            return str.split(regex);
        }
    }
    

    public static String[] lines(String str) {
        if (str == null || str.length() == 0) {
            return new String[0];
        } else {
            String delim = (str.indexOf('\n') >= 0 ? "[\n\r]" : "[\r]");
            return str.split(delim);
        }
    }
    
    public static String[] paragraphs(String str) {
        String pattern = "(?<=(\r\n|\r|\n))([ \\t]*$)+";
        if (str == null || str.length() == 0) {
            return new String[0];
        } else {
            return Pattern.compile(pattern, Pattern.MULTILINE).split(str);
        }
    }
    
    public static String concat(String str1, String str2) {
        return str1.concat(str2);
    }
    
    public static String concat(String[] strs) {
        StringBuilder builder = new StringBuilder();
        for (String str: strs) {
            builder.append(str);
        }
        return builder.toString();
    }
    
    public static String concat(List<String> strs) {
        StringBuilder builder = new StringBuilder();
        for (String str: strs) {
            builder.append(str);
        }
        return builder.toString();
    }
    
    public static List<String> safeLinesFromFile(String filename, String baseDir) throws Redirection {
        List<String> list = new ArrayList<String>();
        FileReader in = null;
        try {
            File file = new File(baseDir + filename);
            if (file.exists() && !file.isDirectory()) {
                if (baseDir.length() > 0 && !file.getCanonicalPath().startsWith(baseDir)) {
                    throw new IllegalAccessException("Attempt to access restricted file");
                }
                in = new FileReader(file);
                StringBuilder sb = new StringBuilder();
                StringBuilder sbTag = new StringBuilder();
                boolean tagOpen = false;

                // parsing logic:
                // 
                //  -- support two modes, normal and tag (tagOpen == true)
                //  -- use two buffers, main buffer for normal mode and tag buffer for tag mode
                //  -- in normal mode, look for newlines, tag start delimiter (<) or end of file
                //      -- if newline or end of file, trim the buffer and add to the line list
                //      -- if tag start, switch to tag mode
                //      -- otherwise, add the character to the main buffer
                //  -- in tag mode, look for tag start (<), tag end (>), or end of file
                //      -- if tag start, encode the original start tag to &lt;, add to main buffer 
                //         and clear tag buffer
                //      -- if end of file, encode the original start tag, trim the tag buffer
                //         and add to line list
                //      -- if newline, replace with a space and add to tag buffer
                //      -- if tag end, validate tag buffer, add to main buffer and switch to
                //         normal mode
                //      -- otherwise, add the characer to the tag buffer
                //  -- tag validation fails when the tag contains an equals sign or the string
                //     "script"     
                //  -- when tag validation fails, the tag start is encoded.
                
                while (true) {
                    int c = in.read();
                    if (tagOpen) {
                        if (c < 0) {
                            sb.append("&lt;");
                            if (sbTag.length() > 1) {
                                sb.append(sbTag.substring(1));
                            }
                            list.add(sb.toString().trim());
                            break;
                        } else if (c == '<') {
                            sb.append("&lt;");
                            if (sbTag.length() > 1) {
                                sb.append(sbTag.substring(1));
                                sbTag.delete(1, sbTag.length());
                            }
                        } else if (c == '>') {
                            sbTag.append(c);
                            sb.append(validateTag(sbTag));
                            sbTag.delete(0, sbTag.length());
                            tagOpen = false;
                        } else {
                            sb.append((char) c);
                        }

                    } else {
                        if (c < 0 || c == '\n') {
                            list.add(sb.toString().trim());
                            if (c < 0) {
                                break;
                            }
                            sb.delete(0, sb.length());
                        } else if (c == '<') {
                            sbTag.append(c);
                            tagOpen = true;
                        } else {
                            sb.append((char) c);
                        }
                    }
                }
            }

        } catch (Exception e) {
            String errmsg = "Exception processing " + filename + ": " + e.toString();
            SiteBuilder.log(errmsg);
            throw new Redirection(Redirection.STANDARD_ERROR, errmsg);
        } finally  {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    ;
                }
            }
        }

        return list;
    }
    
    private static String validateTag(StringBuilder tag) {
        String tagCheck = tag.toString().toLowerCase();
        
        if (tagCheck.length() > 1 && 
                (tagCheck.indexOf("=") > 0 || 
                 tagCheck.indexOf("script") > 0 ||
                 tagCheck.indexOf("style") > 0)) {

            return "&lt;" + tag.substring(1);
        } else if (tag.length() == 1) {
            return "&lt;";
        } else {
            return tag.toString();
        }
    }

    public static String getenv(String varname) {
    	String envvar = null;
        try {
        	envvar = System.getenv(varname);
        } catch (Throwable t) {
        	SiteBuilder.log("Unable to get environment variable " + varname + ": " + t.toString());
        }
        
        return envvar;
    }
    
    
    public static String getFileContents(String fileName) throws Exception {
    	return getFileContents(fileName, 4096);
    }
    
    public static String getFileContents(File file) throws Exception {
        try {
            return getFileContents(new BufferedReader(new FileReader(file), 4096));
        } catch (Exception e) {
            System.err.println("Exception getting contents for file " + file.getName() + ": " + e);
            throw e;
        }
    }

    public static String getFileContents(String fileName, int bufLen) throws Exception {
        try {
            return getFileContents(new BufferedReader(new FileReader(fileName), bufLen));
        } catch (Exception e) {
            System.err.println("Exception getting contents for file " + fileName + ": " + e);
            throw e;
        }
    }

        
    public static String getFileContents(Reader reader) throws Exception {       
  		StringBuilder sb = new StringBuilder();
		for (int c = reader.read(); c >= 0; c = reader.read()) {
			sb.append((char) c);
		}
        return sb.toString();
    }

    public static void writeToFile(File file, String data) throws Exception {
        FileWriter fw = new FileWriter(file, false);
        fw.write(data);
        fw.close();
    }
    
    
    public static void appendToFile(File file, String data) throws Exception {
        FileWriter fw = new FileWriter(file, true);
        fw.write(data);
        fw.close();
    }
    
    
    /** Returns a File object. The file is specified by two strings, base and path.  base is
     *  an absolute URL or directory, while path is a relative path to base.  If either is null,
     *  the value is interpreted to be "." (the current directory). 
     * 
     * @param base
     * @param path
     * @return
     */
    
    public static BentoFile getFile(String base, String path) {
        if (base == null) {
            base = ".";
        }
        if (path == null) {
            return new BentoFile(base);
        } else {
            return new BentoFile(base, path);
        }
    }
    
    public static BentoFile getBentoFile(BentoFile fbase, String base, String path) {
        if (path == null) {
            return (fbase == null ? new BentoFile(base) : new BentoFile(fbase));
        } else {
            return (fbase == null ? new BentoFile(base, path) : new BentoFile(fbase, path));
        }
    }    

    public static String[] dir(String path) {
        File file = new File(path);
        String[] files = file.list();
        return files;
    }

    /** Get the current time in milliseconds elapsed since 1/1/1970 **/
    public static long current_time() {
    	return System.currentTimeMillis();
    }
}

class TokenIterator implements Iterator<String> {

    private StringTokenizer tokenizer;

    public TokenIterator(StringTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public boolean hasNext() {
        return tokenizer.hasMoreTokens();
    }

    public String next() {
        return tokenizer.nextToken();
    }

    public void remove() {
        throw new UnsupportedOperationException("TokenIterator doesn't support remove");
    }
}