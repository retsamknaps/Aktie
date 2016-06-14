package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.Index;

public class NewForceSearcher extends GenericProcessor
{
    private Index index;

    public NewForceSearcher ( Index i )
    {
        index = i;
    }

    /**
        Must set:
        string: creator, community
        number: fragsize
        private: localfile
    */
    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.USR_FORCE_SEARCHER.equals ( type ) )
        {
            index.forceNewSearcher();
            return true;
        }

        return false;
    }

}
