package aktie;

public abstract class GenericNoContextProcessor extends GenericProcessor
{

    @Override
    public void setContext ( Object c )
    {
        throw new RuntimeException ( "No context processor!" );
    }

}
