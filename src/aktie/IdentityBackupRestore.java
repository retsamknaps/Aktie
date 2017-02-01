package aktie;

import java.io.File;

import aktie.index.CObjList;
import aktie.index.Index;

public class IdentityBackupRestore
{

    private Index index;



    public void saveIdentity ( File f )
    {

        CObjList clst = index.getMyIdentities();
        clst.close();

        clst = index.getMySubscriptions();
        clst.close();
    }

}
