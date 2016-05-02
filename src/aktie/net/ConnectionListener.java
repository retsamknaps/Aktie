package aktie.net;

public interface ConnectionListener
{

    public void update ( ConnectionThread ct );

    public void closed ( ConnectionThread ct );

    public void bytesReceived ( long bytes );

    public void bytesSent ( long bytes );

}
