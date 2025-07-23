package ups.vision.clasificador_app;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ScrollView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("clasificador_app");
    }

    // Funciones nativas
    public native String clasificarFigura(byte[] imageData, String datasetContent);
    public native String evaluarSistema(String datasetContent);

    private DrawingView drawingView;
    private Button btnClasificar;
    private Button btnLimpiar;
    private Button btnEvaluar;
    private TextView textResultado;
    private TableLayout tableConfusion;  // NUEVO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        btnClasificar = findViewById(R.id.btnClasificar);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        btnEvaluar = findViewById(R.id.btnEvaluar);
        textResultado = findViewById(R.id.textResultado);
        tableConfusion = findViewById(R.id.tableConfusion); // Vincula la tabla

        btnClasificar.setOnClickListener(v -> procesarYClasificarFigura());
        btnLimpiar.setOnClickListener(v -> limpiarDibujo());
        btnEvaluar.setOnClickListener(v -> evaluarSistema());
    }

    private void procesarYClasificarFigura() {
        Bitmap bitmap = drawingView.getBitmap();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String datasetContent = leerArchivoDesdeAssets();
            String resultado = clasificarFigura(byteArray, datasetContent);
            mostrarResultado("Resultado: " + resultado);
            tableConfusion.removeAllViews();  // Oculta matriz si está visible

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void evaluarSistema() {
        try {
            String datasetContent = leerArchivoDesdeAssets();
            String resultadoEvaluacion = evaluarSistema(datasetContent);
            mostrarMatrizConfusion(resultadoEvaluacion);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al evaluar el sistema", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarResultado(String resultado) {
        textResultado.setText(resultado);
    }

    private void limpiarDibujo() {
        drawingView.limpiar();
        textResultado.setText("Resultado: -");
        tableConfusion.removeAllViews();  // Limpia la tabla
        Toast.makeText(this, "Área de dibujo limpia", Toast.LENGTH_SHORT).show();
    }

    private String leerArchivoDesdeAssets() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getAssets().open("dataset.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al leer el archivo CSV", Toast.LENGTH_SHORT).show();
        }
        return stringBuilder.toString();
    }

    private void mostrarMatrizConfusion(String resultado) {
        tableConfusion.removeAllViews(); // Limpiar tabla anterior

        if (!resultado.contains("Matriz de Confusión")) {
            mostrarResultado(resultado);
            return;
        }

        String[] lineas = resultado.split("\n");
        Map<String, Map<String, String>> matriz = new LinkedHashMap<>();
        List<String> etiquetas = new ArrayList<>();

        for (String linea : lineas) {
            if (linea.startsWith("Matriz")) continue;
            if (linea.startsWith("Precisión:")) break;

            String[] partes = linea.split(":");
            if (partes.length < 2) continue;  // Saltar líneas inválidas

            String etiqueta = partes[0].trim();
            etiquetas.add(etiqueta);
            Map<String, String> fila = new LinkedHashMap<>();
            String[] conteos = partes[1].trim().split(" ");
            for (String c : conteos) {
                String[] kv = c.split("=");
                if (kv.length == 2) {
                    fila.put(kv[0], kv[1]);
                }
            }
            matriz.put(etiqueta, fila);
        }

        // Encabezado
        TableRow header = new TableRow(this);
        header.addView(crearCelda("↘\\↙", true));
        for (String col : etiquetas) {
            header.addView(crearCelda(col, true));
        }
        tableConfusion.addView(header);

        // Filas de datos
        for (String filaEtiqueta : etiquetas) {
            TableRow row = new TableRow(this);
            row.addView(crearCelda(filaEtiqueta, true)); // Etiqueta de fila

            for (String col : etiquetas) {
                String val = matriz.get(filaEtiqueta).getOrDefault(col, "0");
                row.addView(crearCelda(val, false));
            }
            tableConfusion.addView(row);
        }

        // Mostrar precisión
        for (String linea : lineas) {
            if (linea.startsWith("Precisión:")) {
                mostrarResultado(linea);
                break;
            }
        }
    }

    private TextView crearCelda(String texto, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(12, 8, 12, 8);
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        if (bold) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return tv;
    }
}