package aktie.gui.table;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;
import aktie.gui.CObjListGetter;

public class CObjListTableCellLabelProviderTypeDisplayName extends CObjListTableCellLabelProvider
{
    public CObjListTableCellLabelProviderTypeDisplayName ( boolean privateAttribute, String highlightKey )
    {
        super ( CObj.NAME, privateAttribute, SortField.Type.STRING, highlightKey );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            String attribute = null;

            if ( isPrivateAttribute() )
            {
                attribute = co.getPrivateDisplayName();
            }

            else
            {
                attribute = co.getDisplayName();
            }

            if ( attribute != null )
            {
                return attribute;
            }

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
        //System.out.println("CObjListTableCellLabelProviderTypeDisplayName.compare()");
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
