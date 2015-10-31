package aktie.net;

public interface ConnectionListener
{

    public void update ( ConnectionThread ct );

    public void closed ( ConnectionThread ct );

}
