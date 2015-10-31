package aktie.json;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

public class CleanParser
{

    private InputStream InStream;
    private long bytesRead;

    public CleanParser ( InputStream is )
    {
        InStream = is;
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
        int rs = InStream.read();

        if ( rs < 0 ) { throw new IOException ( "EOF0" ); }

        char cr = ( char ) rs;
        bytesRead++;

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
            rs = InStream.read();

            if ( rs < 0 ) { throw new IOException ( "EOF1" ); }

            cr = ( char ) rs;

            bytesRead++;
            sb.append ( cr );

            if ( '\'' == cr ) { isquote0 = !isquote0; }

            if ( '"' == cr ) { isquote1 = !isquote1; }

            if ( !lastesc && !isquote0 && !isquote1 )
            {
                if ( '\\' == cr ) { lastesc = true; }

                if ( '{' == cr ) { pcnt++; }

                if ( '}' == cr ) { pcnt--; }

            }

            else
            {
                lastesc = false;
            }

        }

        JSONTokener t = new JSONTokener ( sb.toString() );
        return new JSONObject ( t );
    }

}
