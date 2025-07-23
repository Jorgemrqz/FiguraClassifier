package ups.vision.clasificador_app;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("clasificador_app");
    }

    // Funciones nativas
    public native String clasificarFigura(byte[] imageData, String datasetContent);
    public native String evaluarSistema(String datasetContent);  // NUEVA

    private DrawingView drawingView;
    private Button btnClasificar;
    private Button btnLimpiar;
    private Button btnEvaluar;           // NUEVO
    private TextView textResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        btnClasificar = findViewById(R.id.btnClasificar);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        btnEvaluar = findViewById(R.id.btnEvaluar);   // NUEVO
        textResultado = findViewById(R.id.textResultado);

        btnClasificar.setOnClickListener(v -> procesarYClasificarFigura());
        btnLimpiar.setOnClickListener(v -> limpiarDibujo());
        btnEvaluar.setOnClickListener(v -> evaluarSistema());  // NUEVO
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

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void evaluarSistema() {
        try {
            String datasetContent = leerArchivoDesdeAssets();
            String resultadoEvaluacion = evaluarSistema(datasetContent);
            mostrarResultado(resultadoEvaluacion);
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
}