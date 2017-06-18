package aktie.gui.table;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;
import aktie.gui.CObjListGetter;
import aktie.index.Index;

public class CObjListTableCellLabelProviderTypeIdentityName extends CObjListTableCellLabelProvider
{

    private Index index;

    public CObjListTableCellLabelProviderTypeIdentityName ( String key, boolean privateAttribute, String highlightKey, Index index )
    {
        super ( key, privateAttribute, SortField.Type.STRING, highlightKey );

        this.index = index;
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            String id = co.getPrivate ( getKey() );

            if ( id == null )
            {
                id = "";
            }

            else
            {
                id = index.getDisplayNameForIdentity ( id );
            }

            return id;
        }

        return "";
    }

    /**
        For this cell label provider, we implement a compare method
        because sorting cannot be done in the lucene index as the
        display string is not the string stored in lucene.
    */
    @Override
    public int compare ( Object e1, Object e2, boolean reverse )
    {
        if ( e1 instanceof CObjListGetter && e2 instanceof CObjListGetter )
        {
            try
            {
                CObj co1 = CObjListGetter.class.cast ( e1 ).getCObj();
                CObj co2 = CObjListGetter.class.cast ( e2 ).getCObj();

                String displayName1;
                String displayName2;

                if ( isPrivateAttribute() )
                {
                    displayName1 = co1.getPrivateDisplayName();
                    displayName2 = co2.getPrivateDisplayName();
                }

                else
                {
                    displayName1 = co1.getDisplayName();
                    displayName2 = co2.getDisplayName();
                }

                if ( displayName1 != null && displayName2 != null )
                {
                    int comp = displayName1.compareToIgnoreCase ( displayName2 );

                    if ( reverse && comp != 0 )
                    {
                        return -comp;
                    }

                    return comp;
                }

            }

            catch ( ClassCastException e )
            {
            }

        }

        return 0;
    }

}
