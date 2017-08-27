package aktie;

import aktie.data.CObj;

public interface CObjProcessor
{

    public void setContext ( Object c );

    public boolean processObj ( Object o );

    public boolean process ( CObj b );

}
