package wh.plus.crm.helper;

import org.springframework.beans.BeanUtils;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

public class NullPropertyUtils {

    /**
     * Zwraca tablicę nazw właściwości, które mają wartość null w obiekcie źródłowym.
     *
     * @param source obiekt do sprawdzenia
     * @return tablica nazw właściwości null
     */
    public static String[] getNullPropertyNames(Object source) {
        if (source == null) {
            return new String[0];
        }

        final PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(source.getClass());

        return Arrays.stream(pds)
                .map(PropertyDescriptor::getName)
                .filter(propertyName -> {
                    try {
                        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(source.getClass(), propertyName);
                        if (pd != null && pd.getReadMethod() != null) {
                            return pd.getReadMethod().invoke(source) == null;
                        }
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toArray(String[]::new);
    }
}
