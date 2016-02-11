package aktie.gui;

import java.util.HashMap;
import java.util.Map;

import aktie.data.CObj;
import aktie.index.Index;

public class IdentityCache
{

    private Index index;
    private Map<String, String> nameMap;

    public IdentityCache ( Index i )
    {
        index = i;
        nameMap = new HashMap<String, String>();
    }

    public String getName ( String id )
    {
        String nm = null;

        synchronized ( nameMap )
        {
            nm = nameMap.get ( id );
        }

        if ( nm == null )
        {
            CObj idnt = index.getIdentity ( id );

            if ( idnt != null )
            {
                nm = idnt.getDisplayName();

                if ( nm != null )
                {
                    synchronized ( nameMap )
                    {
                        nameMap.put ( id, nm );
                    }

                }

            }

        }

        return nm;
    }

}
