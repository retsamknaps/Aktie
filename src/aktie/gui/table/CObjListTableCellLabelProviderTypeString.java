package aktie.gui.table;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;

public class CObjListTableCellLabelProviderTypeString extends CObjListTableCellLabelProvider
{

    public CObjListTableCellLabelProviderTypeString ( String key, boolean privateAttribute, String highlightKey )
    {
        super ( key, privateAttribute, SortField.Type.STRING, highlightKey );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            String attribute = null;

            if ( isPrivateAttribute() )
            {
                attribute = co.getPrivate ( getKey() );
            }

            else
            {
                attribute = co.getString ( getKey() );
            }

            if ( attribute != null )
            {
                return attribute;
            }

        }

        return "";
    }

}
