package ups.vision.clasificador_app;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
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

    // Declarar la función nativa
    public native String clasificarFigura(byte[] imageData, String datasetContent);

    private DrawingView drawingView;
    private Button btnClasificar;
    private Button btnLimpiar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        btnClasificar = findViewById(R.id.btnClasificar);
        btnLimpiar = findViewById(R.id.btnLimpiar);

        // Configuración del botón "Clasificar"
        btnClasificar.setOnClickListener(v -> procesarYClasificarFigura());

        // Configuración del botón "Limpiar"
        btnLimpiar.setOnClickListener(v -> limpiarDibujo());
    }

    private void procesarYClasificarFigura() {
        // Obtener el Bitmap de la figura dibujada
        Bitmap bitmap = drawingView.getBitmap();

        try {
            // Convertir Bitmap a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Leer el archivo CSV desde assets y convertirlo a un String
            String datasetContent = leerArchivoDesdeAssets();

            // Llamar a la función nativa para clasificar la imagen
            String resultado = clasificarFigura(byteArray, datasetContent);

            // Mostrar el resultado de la clasificación
            mostrarResultado(resultado);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarResultado(String resultado) {
        Toast.makeText(this, "Figura clasificada como: " + resultado, Toast.LENGTH_SHORT).show();
    }

    private void limpiarDibujo() {
        // Limpiar el área de dibujo en la vista DrawingView
        drawingView.limpiar();
        Toast.makeText(this, "Área de dibujo limpia", Toast.LENGTH_SHORT).show();
    }

    // Función para leer el archivo CSV desde assets y devolverlo como un String
    private String leerArchivoDesdeAssets() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Acceder al archivo desde assets
            InputStream inputStream = getAssets().open("dataset.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Agregar cada línea del archivo CSV al StringBuilder
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