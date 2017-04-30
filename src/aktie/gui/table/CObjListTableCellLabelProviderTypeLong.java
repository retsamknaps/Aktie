package aktie.gui.table;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;

public class CObjListTableCellLabelProviderTypeLong extends CObjListTableCellLabelProvider
{

    public CObjListTableCellLabelProviderTypeLong ( String key, boolean privateAttribute, String highlightKey )
    {
        super ( key, privateAttribute, SortField.Type.LONG, highlightKey );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            Long attribute = null;

            if ( isPrivateAttribute() )
            {
                attribute = co.getPrivateNumber ( getKey() );
            }

            else
            {
                attribute = co.getNumber ( getKey() );
            }

            if ( attribute != null )
            {
                return attribute.toString();
            }

        }

        return "";
    }

}
