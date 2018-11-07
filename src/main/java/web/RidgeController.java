package web;

import ij.ImagePlus;
import ij.gui.Overlay;
import image.ApplicationMain;
import image.helpers.FileMinion;
import image.helpers.RidgeHelper;
import image.models.RidgeDetection.RidgeLineReport;
import image.models.RidgeDetection.RidgeLinesReport;
import image.models.RidgeDetection.RidgeOptions;
import image.models.RidgeDetection.RidgeResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class RidgeController {
    @Inject
    private ServletContext context;

    private BufferedImage bufferedImage;
    private final String BLANK_IMAGE_PATH = "/resources/blank.png";
    private String thresholdType;
    private FileMinion fileMinion;
    private RidgeOptions ridgeOptions;
    private RidgeResult ridgeResult;
    private final String DIR_PATH = "src/main/webapp/WEB-INF/files/";

    private double sigma = 8.58D;
    private double lowt = 0.00D;
    private double uppt= 0.17D;
    private double minl =0.0D;
    private double maxl = 0.0D;




    @PostConstruct
    public void init() {
        this.ridgeOptions = new RidgeOptions();
        this.fileMinion = new FileMinion();
    }

    public String submitForm() {
        try {

            this.ridgeResult = new RidgeResult();
            final ImagePlus imp = new ImagePlus("theTitle", bufferedImage);

            //Run ridge detection
            RidgeHelper ridgeHelper = new RidgeHelper(imp);
            ridgeHelper.runRidgeDetection(sigma,uppt,lowt,minl,maxl);
            ridgeResult.setResultLines(ridgeHelper.getLines());

            //Create Green Overlay image
            Overlay overlay = ridgeHelper.displayContours();
            imp.setOverlay(overlay);
            ImagePlus imp2 = imp.flatten();
            ridgeResult.setResultImage(imp2);
            ridgeResult.setInitialImage(imp);

            //Create histogram with mean Datas
            ArrayList<Double> meanDatas = ridgeHelper.retrieveMeanData();
            HistogramDataset dataset = new HistogramDataset();
            dataset.setType(HistogramType.FREQUENCY);
            dataset.addSeries("Hist",meanDatas.stream().mapToDouble(Double::doubleValue).toArray(),75);
            JFreeChart barChart = ChartFactory.createHistogram(
                    "Mean Line Width",
                    "Mean Line Width", "Occurrence",
                    dataset, PlotOrientation.VERTICAL,
                    true, true, false);
            ridgeResult.setHistogram(new ImagePlus("myimage",barChart.createBufferedImage(1200,600)));

            //Create report tables
            ArrayList<RidgeLineReport> ridgeLineReports = new ArrayList<>();
            ArrayList<RidgeLinesReport> ridgeLinesReports = new ArrayList<>();
            ridgeHelper.createResultsTable(ridgeLineReports, ridgeLinesReports);
            ridgeResult.setRidgeLineReport(ridgeLineReports);
            ridgeResult.setRidgeLinesReport(ridgeLinesReports);

        } catch (Exception e) {
            e.printStackTrace();
        }
        fileMinion.deleteDirectoryAndFiles(DIR_PATH + getSessionID());
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("ridgeresult", this.ridgeResult);
        return "ridge_result?faces-redirect=true";
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            InputStream inputStream = event.getFile().getInputstream();
            bufferedImage = ImageIO.read(inputStream);
            FacesMessage msg
                    = new FacesMessage("File Description", "file name: "
                    + event.getFile().getFileName() + "file size: "
                    + event.getFile().getSize() / 1024 + " Kb content type: "
                    + event.getFile().getContentType() + "The file was uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (IOException e) {
            e.printStackTrace();
            FacesMessage error = new FacesMessage(FacesMessage.SEVERITY_ERROR, "The files were not uploaded!", "");
            FacesContext.getCurrentInstance().addMessage(null, error);
        }
    }

    private String getSessionID() {
        FacesContext fCtx = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
        return session.getId();
    }

    private DefaultStreamedContent getBlankImage() {
        InputStream is = context.getResourceAsStream(BLANK_IMAGE_PATH);
        return new DefaultStreamedContent(is);
    }


    public DefaultStreamedContent getImgPreview() {
        DefaultStreamedContent imgPreview;
        if (bufferedImage != null) {
            ImagePlus imagePlus = new ImagePlus("theTitle", bufferedImage);
            ApplicationMain applicationMain = new ApplicationMain(imagePlus);
            BufferedImage temp = applicationMain.applyThreshold(this.thresholdType);
            int newWidth = new Double(temp.getWidth() * 0.5).intValue();
            int newHeight = new Double(temp.getHeight() * 0.5).intValue();
            BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = newImage.createGraphics();
            g.drawImage(temp, 0, 0, newWidth, newHeight, null);
            g.dispose();
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            try {
                ImageIO.write(newImage, "png", bas);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] data = bas.toByteArray();
            InputStream is = new ByteArrayInputStream(data);
            imgPreview = new DefaultStreamedContent(is);
        } else {
            imgPreview = this.getBlankImage();
        }
        return imgPreview;

    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public RidgeOptions getRidgeOptions() {
        return ridgeOptions;
    }

    public double getSigma() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    public double getLowt() {
        return lowt;
    }

    public void setLowt(double lowt) {
        this.lowt = lowt;
    }

    public double getUppt() {
        return uppt;
    }

    public void setUppt(double uppt) {
        this.uppt = uppt;
    }

    public double getMinl() {
        return minl;
    }

    public void setMinl(double minl) {
        this.minl = minl;
    }

    public double getMaxl() {
        return maxl;
    }

    public void setMaxl(double maxl) {
        this.maxl = maxl;
    }
}