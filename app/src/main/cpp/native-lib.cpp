#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "Clasificador"

cv::Mat preprocesarImagen(const cv::Mat &imagenOriginal) {
    cv::Mat imagenGrises;
    cv::cvtColor(imagenOriginal, imagenGrises, cv::COLOR_BGR2GRAY);

    // Paso 1: Aplicar filtro Gaussiano para suavizar la imagen y reducir el ruido
    cv::Mat imagenFiltrada;
    cv::GaussianBlur(imagenGrises, imagenFiltrada, cv::Size(5, 5), 0);

    // Paso 2: Usar un umbral adaptativo para manejar diferentes iluminaciones
    cv::Mat imagenBinaria;
    cv::adaptiveThreshold(imagenFiltrada, imagenBinaria, 255, cv::ADAPTIVE_THRESH_MEAN_C, cv::THRESH_BINARY_INV, 11, 2);

    // Paso 3: Encontrar contornos y eliminar los pequeños contornos (ruido)
    std::vector<std::vector<cv::Point>> contornos;
    std::vector<cv::Vec4i> jerarquia;
    cv::findContours(imagenBinaria, contornos, jerarquia, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

    cv::Mat imagenLimpia = cv::Mat::zeros(imagenBinaria.size(), CV_8UC1);

    for (size_t i = 0; i < contornos.size(); i++) {
        // Eliminar contornos pequeños
        if (cv::contourArea(contornos[i]) > 500) {
            cv::drawContours(imagenLimpia, contornos, (int)i, cv::Scalar(255), cv::FILLED);
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Imagen preprocesada con éxito.");
    return imagenLimpia;
}

void calcularMomentosYFirma(const cv::Mat &imagen, std::vector<double>& hu, std::vector<float>& shapeSignature) {
    // Encontrar los contornos de la imagen
    std::vector<std::vector<cv::Point>> contornos;
    std::vector<cv::Vec4i> jerarquia;
    cv::findContours(imagen, contornos, jerarquia, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

    if (contornos.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error: No se encontraron contornos en la imagen.");
        return;
    }

    // Calcular los momentos de Hu
    cv::Moments momentos = cv::moments(contornos[0]);
    double huArray[7];
    cv::HuMoments(momentos, huArray);

    // Copiar los momentos de Hu al vector
    for (int i = 0; i < 7; i++) {
        hu.push_back(huArray[i]);
    }

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Momentos de Hu calculados: %f, %f, %f, %f, %f, %f, %f",
                        hu[0], hu[1], hu[2], hu[3], hu[4], hu[5], hu[6]);

    // Cálculo de la Shape Signature (coordenadas X e Y de los contornos)
    std::vector<cv::Point> contorno = contornos[0];
    for (auto& p : contorno) {
        shapeSignature.push_back(p.x);
        shapeSignature.push_back(p.y);
    }

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Shape Signature calculada.");
}

void cargarDescriptores(const std::string& datasetContent, std::vector<std::vector<double>>& descriptores, std::vector<std::string>& etiquetas) {
    std::istringstream archivo(datasetContent);  // Convertir la cadena a un stream
    std::string linea;

    // Leer la cabecera
    std::getline(archivo, linea);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Cabecera: %s", linea.c_str());

    int lineNumber = 1; // Para contar las líneas y depurar más fácilmente
    while (std::getline(archivo, linea)) {
        lineNumber++;

        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Leyendo línea %d: %s", lineNumber, linea.c_str());

        std::vector<double> descriptor;
        std::string tipoFigura;
        size_t pos = 0;

        // Obtener el tipo de figura
        pos = linea.find(',');
        if (pos == std::string::npos) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error al procesar la línea %d: %s (No se encontró una coma)", lineNumber, linea.c_str());
            continue; // Salta esta línea si no se puede procesar
        }
        tipoFigura = linea.substr(0, pos);
        linea.erase(0, pos + 1);

        // Leer los momentos de Hu
        for (int i = 0; i < 7; ++i) {
            pos = linea.find(',');
            if (pos == std::string::npos) {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error al procesar la línea %d: %s (No se encontró el número %d)", lineNumber, linea.c_str(), i + 1);
                continue; // Salta esta línea si no se puede procesar
            }
            descriptor.push_back(std::stod(linea.substr(0, pos)));
            linea.erase(0, pos + 1);
        }

        // Guardar los descriptores y la etiqueta (tipo de figura)
        descriptores.push_back(descriptor);
        etiquetas.push_back(tipoFigura);
    }

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Descriptores cargados desde el contenido CSV.");
}

double calcularDistanciaEuclidiana(const std::vector<double>& descriptor1, const std::vector<double>& descriptor2) {
    double suma = 0;
    for (size_t i = 0; i < descriptor1.size(); ++i) {
        suma += std::pow(descriptor1[i] - descriptor2[i], 2);
    }
    return std::sqrt(suma);
}

extern "C" JNIEXPORT jstring JNICALL
Java_ups_vision_clasificador_1app_MainActivity_clasificarFigura(JNIEnv* env, jobject /* this */, jbyteArray imageData, jstring datasetContentJ) {
    const char* datasetStr = env->GetStringUTFChars(datasetContentJ, 0);
    std::string dataset(datasetStr);
    env->ReleaseStringUTFChars(datasetContentJ, datasetStr);

    // Convertir la imagen desde jbyteArray a cv::Mat
    jsize length = env->GetArrayLength(imageData);
    jbyte* imageBytes = env->GetByteArrayElements(imageData, 0);
    std::vector<uchar> buf(imageBytes, imageBytes + length);
    cv::Mat imagen = cv::imdecode(buf, cv::IMREAD_COLOR);
    env->ReleaseByteArrayElements(imageData, imageBytes, 0);

    if (imagen.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error al cargar la imagen.");
        return env->NewStringUTF("Error al cargar la imagen.");
    }

    // Preprocesamiento
    cv::Mat imagenPreprocesada = preprocesarImagen(imagen);

    std::vector<double> hu;
    std::vector<float> shapeSignature;
    calcularMomentosYFirma(imagenPreprocesada, hu, shapeSignature);

    if (hu.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "No se pudieron calcular momentos de Hu.");
        return env->NewStringUTF("Error: sin contornos válidos.");
    }

    // Cargar descriptores desde datasetContent (ya no se usa ruta, sino contenido CSV)
    std::istringstream archivo(dataset);
    std::string linea;

    // Leer cabecera
    std::getline(archivo, linea);

    std::vector<std::vector<double>> descriptores;
    std::vector<std::string> etiquetas;

    int lineNumber = 1;
    while (std::getline(archivo, linea)) {
        lineNumber++;
        std::vector<double> descriptor;
        std::string tipoFigura;
        size_t pos = 0;

        // TipoFigura
        pos = linea.find(',');
        if (pos == std::string::npos) continue;
        tipoFigura = linea.substr(0, pos);
        linea.erase(0, pos + 1);

        // Momentos 1–7
        bool formatoValido = true;
        for (int i = 0; i < 7; ++i) {
            pos = linea.find(',');
            if (pos == std::string::npos) {
                formatoValido = false;
                break;
            }
            try {
                descriptor.push_back(std::stod(linea.substr(0, pos)));
            } catch (...) {
                formatoValido = false;
                break;
            }
            linea.erase(0, pos + 1);
        }

        if (formatoValido && descriptor.size() == 7) {
            descriptores.push_back(descriptor);
            etiquetas.push_back(tipoFigura);
        } else {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Línea %d inválida: %s", lineNumber, tipoFigura.c_str());
        }
    }

    if (descriptores.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "No se cargaron descriptores válidos.");
        return env->NewStringUTF("Error: dataset vacío o inválido.");
    }

    // Clasificación: comparar con todos los descriptores
    double menorDistancia = std::numeric_limits<double>::max();
    std::string tipoFigura = "";

    for (size_t i = 0; i < descriptores.size(); ++i) {
        double distancia = calcularDistanciaEuclidiana(hu, descriptores[i]);
        if (distancia < menorDistancia) {
            menorDistancia = distancia;
            tipoFigura = etiquetas[i];
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Figura clasificada como: %s", tipoFigura.c_str());
    return env->NewStringUTF(tipoFigura.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_ups_vision_clasificador_1app_MainActivity_evaluarSistema(JNIEnv* env, jobject, jstring datasetContentJ) {
    const char* datasetStr = env->GetStringUTFChars(datasetContentJ, 0);
    std::string dataset(datasetStr);
    env->ReleaseStringUTFChars(datasetContentJ, datasetStr);

    std::istringstream archivo(dataset);
    std::string linea;

    std::getline(archivo, linea);  // Leer cabecera

    std::vector<std::vector<double>> descriptores;
    std::vector<std::string> etiquetas;

    int lineNumber = 1;
    while (std::getline(archivo, linea)) {
        lineNumber++;
        std::vector<double> descriptor;
        std::string tipoFigura;
        size_t pos = 0;

        // Leer etiqueta
        pos = linea.find(',');
        if (pos == std::string::npos) continue;
        tipoFigura = linea.substr(0, pos);
        linea.erase(0, pos + 1);

        // Leer 7 momentos de Hu
        bool valido = true;
        for (int i = 0; i < 7; ++i) {
            pos = linea.find(',');
            if (pos == std::string::npos) { valido = false; break; }
            descriptor.push_back(std::stod(linea.substr(0, pos)));
            linea.erase(0, pos + 1);
        }

        if (valido && descriptor.size() == 7) {
            descriptores.push_back(descriptor);
            etiquetas.push_back(tipoFigura);
        }
    }

    std::vector<std::string> predichos;
    int correctos = 0;

    // Clasificación Leave-One-Out
    for (size_t i = 0; i < descriptores.size(); ++i) {
        double minDist = std::numeric_limits<double>::max();
        std::string prediccion;

        for (size_t j = 0; j < descriptores.size(); ++j) {
            if (i == j) continue;
            double dist = calcularDistanciaEuclidiana(descriptores[i], descriptores[j]);
            if (dist < minDist) {
                minDist = dist;
                prediccion = etiquetas[j];
            }
        }

        predichos.push_back(prediccion);
        if (prediccion == etiquetas[i]) correctos++;
    }

    // Construir matriz de confusión
    std::map<std::string, std::map<std::string, int>> matriz;
    for (size_t i = 0; i < etiquetas.size(); ++i) {
        matriz[etiquetas[i]][predichos[i]]++;
    }

    std::ostringstream out;
    out << "Matriz de Confusión:\n";
    for (const auto& fila : matriz) {
        out << fila.first << ": ";
        for (const auto& col : fila.second) {
            out << col.first << "=" << col.second << " ";
        }
        out << "\n";
    }

    float precision = (float)correctos / etiquetas.size();
    out << "\nPrecisión: " << precision;

    return env->NewStringUTF(out.str().c_str());
}
