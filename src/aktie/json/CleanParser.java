package aktie.json;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

public class CleanParser
{

    private InputStream InStream;
    private long bytesRead;
    private boolean permissive;

    public CleanParser ( InputStream is )
    {
        InStream = is;
    }

    public void setPermissive ( boolean p )
    {
        permissive = p;
    }

    public InputStream getInStream()
    {
        return InStream;
    }

    public long getBytesRead()
    {
        return bytesRead;
    }

    public JSONObject next() throws IOException
    {
        bytesRead = 0;
        StringBuilder sb = new StringBuilder();
        int pcnt = 1;
        char cr = 0;

        do
        {
            int rs = InStream.read();

            if ( rs < 0 ) { throw new IOException ( "EOF0" ); }

            cr = ( char ) rs;
            bytesRead++;

        }

        while ( cr != '{' && permissive );

        if ( '{' != cr )
        {
            throw new IOException ( "Must be {" );
        }

        sb.append ( cr );
        boolean lastesc = false;
        boolean isquote0 = false;
        boolean isquote1 = false;

        while ( pcnt > 0 )
        {
            int rs = InStream.read();

            if ( rs < 0 ) { throw new IOException ( "EOF1" ); }

            cr = ( char ) rs;

            bytesRead++;
            sb.append ( cr );

            if ( !lastesc && !isquote1 && '\'' == cr ) { isquote0 = !isquote0; }

            if ( !lastesc && !isquote0 && '"' == cr ) { isquote1 = !isquote1; }

            if ( !lastesc && !isquote0 && !isquote1 )
            {

                if ( '{' == cr ) { pcnt++; }

                if ( '}' == cr ) { pcnt--; }

            }

            if ( !lastesc && '\\' == cr ) { lastesc = true; }

            else { lastesc = false; }

        }

        String js = sb.toString();
        JSONTokener t = new JSONTokener ( js );
        return new JSONObject ( t );
    }

}
