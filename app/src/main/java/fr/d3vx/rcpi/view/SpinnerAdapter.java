/**
 * Created by hacketo on 28/05/18.
 */

package fr.d3vx.rcpi.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import fr.d3vx.rcpi.R;


/**
 * @see : https://stackoverflow.com/a/20889019/2538473
 * Only to have multiline on the spinner dropdown ...
 */
public class SpinnerAdapter extends ArrayAdapter<String> {

    private List<String> objects;
    private LayoutInflater inflater;
    private int oddRowColor = Color.parseColor("#E7E3D1");
    private int evenRowColor = Color.parseColor("#F8F6E9");

    public SpinnerAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        this.objects = objects;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    static class ViewHolder {
        public TextView nameTextView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewInternal(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getViewInternal(position, convertView, parent, true);
    }

    private View getViewInternal(int position, View convertView, ViewGroup parent, boolean isDropdownView) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.spinner_item, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.nameTextView = (TextView) view.findViewById(R.id.spinner_item);
            view.setTag(viewHolder);
        }
        if (isDropdownView) {
            view.setBackgroundColor(position % 2 == 0 ? evenRowColor : oddRowColor);
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        String model = objects.get(position);
        holder.nameTextView.setText(model);
        return view;
    }

}
