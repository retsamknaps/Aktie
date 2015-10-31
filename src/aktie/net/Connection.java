package aktie.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Connection
{

    public void connect() throws IOException;

    public InputStream getInputStream();

    public OutputStream getOutputStream();

    public void close();

}
