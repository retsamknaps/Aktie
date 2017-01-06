package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.Index;

public class InDigProcessor extends GenericProcessor
{

    private ConnectionThread conThread;
    private Index index;

    public InDigProcessor ( ConnectionThread ct, Index i )
    {
        conThread = ct;
        index = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.OBJDIG.equals ( b.getType() ) )
        {
            String d = b.getDig();
            System.out.println ( "DIG FOR SEQ: " + d );

            if ( d != null )
            {
                CObj o = index.getByDig ( d );
                System.out.println ( "OBJ FOUND FOR DIG " + d + " " + o );

                if ( o == null )
                {
                    conThread.addReqDig ( d );
                }

            }

            return true;
        }

        return false;
    }

}
