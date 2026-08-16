package mx.gob.impepac.redicap.vision

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.sqrt

/** Se fija en MainActivity al iniciar la app, según si OpenCVLoader.initLocal() tuvo éxito. */
var opencvReady: Boolean = false

private const val TAG = "RecortadorActa"
private const val DETECCION_LADO_MAX = 800.0
private const val SALIDA_LADO_MAX = 2400.0
private const val AREA_MINIMA_RELATIVA = 0.20

/**
 * Detecta el documento en la foto (Canny + contornos), lo recorta con una transformación
 * de perspectiva (homografía) y optimiza contraste/nitidez (DFR R1).
 *
 * Nunca lanza: ante cualquier fallo o si no se encuentra un contorno de 4 puntos confiable,
 * devuelve el bitmap original sin modificar — no hay que bloquear la captura en campo.
 */
fun detectarYRecortarActa(bitmap: Bitmap): Bitmap {
    if (!opencvReady) return bitmap

    val original = Mat()
    try {
        Utils.bitmapToMat(bitmap, original)

        val ladoOriginal = max(original.cols(), original.rows()).toDouble()
        val escala = if (ladoOriginal > DETECCION_LADO_MAX) DETECCION_LADO_MAX / ladoOriginal else 1.0

        val chico = Mat()
        val gris = Mat()
        val borroso = Mat()
        val bordes = Mat()
        try {
            if (escala < 1.0) {
                Imgproc.resize(original, chico, Size(), escala, escala, Imgproc.INTER_AREA)
            } else {
                original.copyTo(chico)
            }

            Imgproc.cvtColor(chico, gris, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gris, borroso, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(borroso, bordes, 75.0, 200.0)
            Imgproc.dilate(bordes, bordes, Mat())

            val cuadrilatero = hallarMejorCuadrilatero(bordes, chico.cols() * chico.rows())
                ?: return bitmap

            val esquinas = cuadrilatero.toArray()
                .map { Point(it.x / escala, it.y / escala) }
                .let(::ordenarEsquinas)
            cuadrilatero.release()

            val recortado = recortarPorHomografia(original, esquinas)
            val optimizado = optimizarContrasteYNitidez(recortado)
            recortado.release()

            val resultado = Bitmap.createBitmap(optimizado.cols(), optimizado.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(optimizado, resultado)
            optimizado.release()
            return resultado
        } finally {
            chico.release()
            gris.release()
            borroso.release()
            bordes.release()
        }
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo recortar automáticamente, se usa la imagen original", e)
        return bitmap
    } finally {
        original.release()
    }
}

/** Busca, entre los contornos detectados, el cuadrilátero convexo de mayor área. */
private fun hallarMejorCuadrilatero(bordes: Mat, areaFrame: Int): MatOfPoint2f? {
    val contornos = mutableListOf<MatOfPoint>()
    val jerarquia = Mat()
    try {
        Imgproc.findContours(bordes, contornos, jerarquia, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        var mejor: MatOfPoint2f? = null
        var mejorArea = 0.0
        for (contorno in contornos) {
            // OpenCV 5.0 movió contourArea/arcLength/approxPolyDP/isContourConvex/
            // getPerspectiveTransform de Imgproc a la nueva clase Geometry.
            val area = Geometry.contourArea(contorno)
            if (area < areaFrame * AREA_MINIMA_RELATIVA || area <= mejorArea) {
                contorno.release()
                continue
            }

            val contorno2f = MatOfPoint2f(*contorno.toArray())
            val perimetro = Geometry.arcLength(contorno2f, true)
            val approx = MatOfPoint2f()
            Geometry.approxPolyDP(contorno2f, approx, 0.02 * perimetro, true)
            contorno2f.release()
            contorno.release()

            val approxEntero = MatOfPoint(*approx.toArray())
            val esConvexo = approx.total() == 4L && Geometry.isContourConvex(approxEntero)
            approxEntero.release()

            if (esConvexo) {
                mejor?.release()
                mejor = approx
                mejorArea = area
            } else {
                approx.release()
            }
        }
        return mejor
    } finally {
        jerarquia.release()
    }
}

/** Ordena 4 puntos como [top-left, top-right, bottom-right, bottom-left]. */
private fun ordenarEsquinas(puntos: List<Point>): Array<Point> {
    val sumas = puntos.map { it.x + it.y }
    val diffs = puntos.map { it.x - it.y }
    val tl = puntos[sumas.indexOf(sumas.min())]
    val br = puntos[sumas.indexOf(sumas.max())]
    val tr = puntos[diffs.indexOf(diffs.max())]
    val bl = puntos[diffs.indexOf(diffs.min())]
    return arrayOf(tl, tr, br, bl)
}

private fun distancia(a: Point, b: Point): Double =
    sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

/** Aplica getPerspectiveTransform + warpPerspective (homografía) contra la imagen a resolución completa. */
private fun recortarPorHomografia(original: Mat, esquinas: Array<Point>): Mat {
    val (tl, tr, br, bl) = esquinas
    var ancho = max(distancia(br, bl), distancia(tr, tl))
    var alto = max(distancia(tr, br), distancia(tl, bl))
    val ladoMayor = max(ancho, alto)
    if (ladoMayor > SALIDA_LADO_MAX) {
        val factor = SALIDA_LADO_MAX / ladoMayor
        ancho *= factor
        alto *= factor
    }

    val origen = MatOfPoint2f(tl, tr, br, bl)
    val destino = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(ancho, 0.0),
        Point(ancho, alto),
        Point(0.0, alto),
    )
    val homografia = Geometry.getPerspectiveTransform(origen, destino)
    val recortado = Mat()
    try {
        Imgproc.warpPerspective(original, recortado, homografia, Size(ancho, alto))
    } finally {
        origen.release()
        destino.release()
        homografia.release()
    }
    return recortado
}

/** CLAHE (contraste local) sobre el canal de luminancia + unsharp mask ligero (nitidez). */
private fun optimizarContrasteYNitidez(src: Mat): Mat {
    val bgr = Mat()
    val lab = Mat()
    val canales = ArrayList<Mat>()
    try {
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)
        Imgproc.cvtColor(bgr, lab, Imgproc.COLOR_BGR2Lab)
        Core.split(lab, canales)

        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(canales[0], canales[0])
        Core.merge(canales, lab)

        val contrastado = Mat()
        Imgproc.cvtColor(lab, contrastado, Imgproc.COLOR_Lab2BGR)
        try {
            val borroso = Mat()
            val nitido = Mat()
            try {
                Imgproc.GaussianBlur(contrastado, borroso, Size(0.0, 0.0), 3.0)
                Core.addWeighted(contrastado, 1.5, borroso, -0.5, 0.0, nitido)

                val resultado = Mat()
                Imgproc.cvtColor(nitido, resultado, Imgproc.COLOR_BGR2RGBA)
                return resultado
            } finally {
                borroso.release()
                nitido.release()
            }
        } finally {
            contrastado.release()
        }
    } finally {
        bgr.release()
        lab.release()
        canales.forEach { it.release() }
    }
}
