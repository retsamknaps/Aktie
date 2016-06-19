package aktie.utils;

import aktie.crypto.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.digests.SHA256Digest;

public class FUtils
{

    // One or more arbitrary characters, at the end
    // a dot followed by at least one word character.
    // Group contains dot and word characters at the end.
    public static final Pattern GET_FILE_EXT_PATTERN = Pattern.compile( "^.+(\\.[a-zA-Z_0-9]+)$" );
    
    // A dot followed by at least one word character.
    public static final Pattern IS_FILE_EXT_PATTERN = Pattern.compile( "^\\.[a-zA-Z_0-9]+$" );
	
    public static File createTestFile ( File dir, long size ) throws IOException
    {
        File f = null;

        if ( dir != null )
        {
            f = File.createTempFile ( "junkfile", ".dat", dir );
        }

        else
        {
            f = File.createTempFile ( "junkfile", ".dat" );
        }

        FileOutputStream fos = new FileOutputStream ( f );
        byte tb[] = new byte[1024];

        while ( size > 0 )
        {
            Utils.Random.nextBytes ( tb );
            size -= tb.length;
            fos.write ( tb );
        }

        fos.close();
        return f;
    }

    public static String digWholeFile ( String str )
    {
        String dig = null;

        try
        {
            File f = new File ( str );
            SHA256Digest fulldig = new SHA256Digest();
            byte buf[] = new byte[1024];
            FileInputStream fis = new FileInputStream ( f );
            long idx = 0;

            while ( idx < f.length() )
            {
                int rlen = fis.read ( buf, 0, buf.length );

                if ( rlen > 0 )
                {
                    fulldig.update ( buf, 0, rlen );
                    idx += rlen;
                }

            }

            fis.close();

            byte fulldigb[] = new byte[fulldig.getDigestSize()];
            fulldig.doFinal ( fulldigb, 0 );

            dig = Utils.toString ( fulldigb );

        }

        catch ( Exception e )
        {
        }

        return dig;
    }

    @SuppressWarnings ( "resource" )
    public static void copy ( File s, File t ) throws IOException
    {
        if ( !s.equals ( t ) )
        {
            FileInputStream fis = new FileInputStream ( s );
            FileOutputStream fos = new FileOutputStream ( t );
            FileChannel foc = fos.getChannel();
            FileChannel fic = fis.getChannel();
            long tc = fic.transferTo ( 0, fic.size(), foc );

            while ( tc < fic.size() )
            {
                long mb = fic.transferTo ( tc, fic.size() - tc, foc );

                if ( mb > 0 )
                {
                    tc += mb;
                }

            }

            fic.close();
            foc.close();
        }

    }

    public static boolean diff ( File f0, File f1 ) throws IOException
    {
        if ( f0.length() != f1.length() ) { return false; }

        boolean r = true;
        FileInputStream fi0 = new FileInputStream ( f0 );
        FileInputStream fi1 = new FileInputStream ( f1 );
        int v0 = fi0.read();
        int v1 = fi1.read();

        if ( v0 != v1 ) { r = false; }

        while ( v0 >= 0 && v1 >= 0 && r )
        {
            v0 = fi0.read();
            v1 = fi1.read();

            if ( v0 != v1 ) { r = false; }

        }

        fi0.close();
        fi1.close();
        return r;
    }

    public static void deleteDir ( File f )
    {
        if ( f.exists() )
        {
            if ( f.isDirectory() )
            {
                File l[] = f.listFiles();

                for ( File nf : l )
                {
                    deleteDir ( nf );
                }

            }

            f.delete();
        }

    }
    
    public static String getFileExtension ( String fileName )
    {
    	Matcher m = GET_FILE_EXT_PATTERN.matcher ( fileName );

 		if ( m.matches() )
 		{
 			return m.group(1);
 		}
 		
 		return null;
    }
    
    public static boolean isFileExtension ( String str )
    {
    	Matcher m = IS_FILE_EXT_PATTERN.matcher ( str );
    	return m.matches();
    }

}
