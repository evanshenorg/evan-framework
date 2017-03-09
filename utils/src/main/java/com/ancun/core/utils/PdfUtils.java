package com.ancun.core.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.google.common.base.Joiner;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Pdf工具类<br>
 * 功能包括：1、模板生成pdf 2、pdf合并 3、pdf打水印 4、pdf生成swf
 * <p/>
 * create at 2015年6月17日 下午4:01:27
 *
 * @author <a href="mailto:shenwei@ancun.com">ShenWei</a>
 * @version %I%, %G%
 * @since 1.0.6
 */
public class PdfUtils {
    private static final Logger logger = LoggerFactory.getLogger(PdfUtils.class);
    // private static final int BSIZE = 1024;
    private static final String ENCODE = "UTF-8";

    /**
     * 根据velocity模板生成pdf
     *
     * @param outputFile
     * @param map
     * @param template
     * @throws PdfConvertException <p/>
     *                             author: ShenWei<br>
     *                             create at 2015年6月17日 下午4:45:38
     */
    public static void templateToPdf(String fontPath,String outputFile, Map<String, Object> map, String template)
            throws PdfConvertException {
        String content = VelocityUtils.mergeTemplateFromClassPath(template, map, ENCODE);
        createPdf(fontPath,content, outputFile);
    }

    /**
     * 根据velocity模板生成pdf
     *
     * @param outputFile
     * @param map
     * @param template
     * @param baseDir
     * @throws PdfConvertException <p/>
     *                             author: ShenWei<br>
     *                             create at 2015年6月17日 下午4:46:06
     */
    public static void templateToPdf(String fontPath,String outputFile, Map<String, Object> map, String template, String baseDir)
            throws PdfConvertException {
        String content = VelocityUtils.mergeTemplateFromFilePath(baseDir, template, map, ENCODE);
        createPdf(fontPath,content, outputFile);
    }

    /**
     * @param inputHtmlFile
     * @param outputPdfFile
     * @throws PdfConvertException <p/>
     *                             author: ShenWei<br>
     *                             create at 2015年6月27日 下午2:31:32
     */

    public static void htmlToPdf(String fontPath,String inputHtmlFile, String outputPdfFile) throws PdfConvertException {
        String content;
        try {
            content = FileUtils.readFileToString(new File(inputHtmlFile));
        } catch (IOException e) {
            throw new PdfConvertException(
                    "Create pdf fail, output [" + inputHtmlFile + "] is invalid," + e.getMessage(), e);
        }

        FileOutputStream os = getFileOutputStream(outputPdfFile);

        try {
            doCreatePdf(fontPath,content, os);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        //
        //
        // try {
        // ITextRenderer renderer = new ITextRenderer();
        // ITextFontResolver fontResolver = renderer.getFontResolver();
        // renderer.setDocument(new File(inputHtmlFile));
        //
        // fontResolver.addFont(getSysConfig().get(SysConfig.KEY_SYSTEM_PDF_FONT),
        // BaseFont.IDENTITY_H,
        // BaseFont.NOT_EMBEDDED);
        //
        // renderer.layout();
        // renderer.createPDF(os);
        // } catch (DocumentException e) {
        // throw new PdfConvertException(e.getMessage(), e);
        // } catch (Exception e) {
        // throw new PdfConvertException(e.getMessage(), e);
        // }
    }

    /**
     * @param content
     * @param outputFile
     * @throws PdfConvertException
     */
    public static void createPdf(String fontPath,String content, String outputFile) throws PdfConvertException {
        FileOutputStream os = getFileOutputStream(outputFile);

        if (os == null) {
            throw new PdfConvertException("Create pdf fail, output is [" + outputFile + "] is invalid");
        }

        logger.info("Create pdf, pdf file is [{}] ", outputFile);
        // Document document = new Document();

        int count = 0;
        PdfConvertException pdfConvertException = null;
        try {
            while (count < 5) {// 失败重试5次
                try {
                    doCreatePdf(fontPath,content, os);
                    break;
                } catch (PdfConvertException ex) {
                    pdfConvertException = ex;
                }
                count++;
            }
        } finally {
            // document.close();
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        if (pdfConvertException != null) {
            throw new PdfConvertException(
                    "Create pdf fail, output is [" + outputFile + "] is not exists," + pdfConvertException.getMessage(),
                    pdfConvertException);
        }
    }

    private static FileOutputStream getFileOutputStream(String outputFile) throws PdfConvertException {
        FileUtils.mkdirs(outputFile);

        FileOutputStream os = null;
        int count = 0;
        FileNotFoundException fileNotFoundException = null;
        while (count < 5) {// 失败重试5次
            try {
                os = new FileOutputStream(outputFile);
                break;
            } catch (FileNotFoundException e) {
                fileNotFoundException = e;
            }
            count++;
        }

        if (fileNotFoundException != null) {
            throw new PdfConvertException(
                    "Create pdf fail, output is [" + outputFile + "] is invalid," + fileNotFoundException.getMessage(),
                    fileNotFoundException);
        }

        return os;
    }

    private static void doCreatePdf(String fontPath,String content, FileOutputStream os) throws PdfConvertException {
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(content);

            // DocumentBuilder builder =
            // DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // org.w3c.dom.Document doc = builder.parse(new
            // ByteArrayInputStream(content.getBytes()));//
            // renderer.setDocument(doc, null);

            ITextFontResolver fontResolver = renderer.getFontResolver();
            fontResolver.addFont(fontPath, BaseFont.IDENTITY_H,BaseFont.NOT_EMBEDDED);

            renderer.layout();
            renderer.createPDF(os);
        } catch (DocumentException e) {
            throw new PdfConvertException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException(e.getMessage(), e);
        } catch (Exception e) {
            throw new PdfConvertException(e.getMessage(), e);
        }
    }

    /**
     * pdf打水印
     *
     * @param inputFile  图片的位置
     * @param imageFile  图片的位置
     * @param outputFile 输出的位置
     * @throws PdfConvertException
     */
    public static void addWaterSign(String inputFile, String imageFile, String outputFile) throws PdfConvertException {
        addWaterSign(inputFile, imageFile, outputFile, 0.7, 0.7);
    }

    /**
     * 打水印
     *
     * @param inputFile
     * @param imageFile
     * @param outputFile
     * @param widthRatio  宽的比例，0.75 则为页面宽的4分3处
     * @param heightRatio 高的比例
     *                    <p/>
     *                    author: xyy create at 2015年4月16日 下午2:31:40
     * @throws PdfConvertException
     */
    public static void addWaterSign(String inputFile, String imageFile, String outputFile, double widthRatio,
                                    double heightRatio) throws PdfConvertException {
        FileUtils.mkdirs(outputFile);

        PdfReader reader = null;
        try {
            reader = new PdfReader(inputFile);
        } catch (IOException e) {
            throw new PdfConvertException("Pdf file [" + inputFile + "] is not exists!", e);
        }
        Image image = null;
        try {
            image = Image.getInstance(imageFile);
        } catch (BadElementException e) {
            throw new PdfConvertException("Image file [" + imageFile + "] is invalid," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException("Image file [" + imageFile + "] is not exists," + e.getMessage(), e);
        }

        PdfStamper stamper = null;
        try {
            logger.info("Add water sign to pdf, pdf file is [{}], water sign image is [{}],output file is [{}]",
                    inputFile, imageFile, outputFile);
            stamper = new PdfStamper(reader, new FileOutputStream(outputFile));
            int pages = reader.getNumberOfPages();// 页数
            for (int i = 1; i < pages + 1; i++) {
                PdfContentByte under = stamper.getOverContent(i);
                float width = reader.getPageSize(i).getWidth();
                float height = reader.getPageSize(i).getHeight();
                image.setAbsolutePosition((float) (width * 0.7), (float) (height * 0.7));// 图片位置
                under.addImage(image);// 添加水印图片
            }
        } catch (DocumentException e) {
            throw new PdfConvertException(
                    "PDF打水印失败, pdf file [" + inputFile + "], image file [" + imageFile + "]," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException(
                    "PDF打水印失败, pdf file [" + inputFile + "], image file [" + imageFile + "]," + e.getMessage(), e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (DocumentException e) {
                    logger.error(e.getMessage(), e);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (null != reader) {
                reader.close();
            }
        }
    }

    public static void addWaterSign(String inputFile, String logoImageFile, String dateImageFile, String outputFile,
                                    String date) throws PdfConvertException {
        addWaterSign(null, inputFile, logoImageFile, dateImageFile, outputFile, date);
    }

    /**
     * inputStream 为Pdf输入流
     */
    public static void addWaterSign(InputStream inputStream, String inputFile, String logoImageFile,
                                    String dateImageFile, String outputFile, String date) throws PdfConvertException {
        addWaterSign(inputStream, inputFile, logoImageFile, dateImageFile, outputFile, date, 0.0, 0.0);
    }

    /**
     * inputStream 为Pdf输入流
     */
    public static void addWaterSign(InputStream inputStream, String inputFile, String logoImageFile,
                                    String dateImageFile, String outputFile, String date, double widthRatio, double heightRatio)
            throws PdfConvertException {
        PdfReader reader = null;
        if (0.0 == widthRatio) {
            widthRatio = 0.63;
        }
        if (0.0 == heightRatio) {
            heightRatio = 0.78;
        }

        if (inputStream != null) {
            try {
                reader = new PdfReader(inputStreamToByte(inputStream));
            } catch (IOException e) {
                throw new PdfConvertException("Pdf add water Sign error," + e.getMessage(), e);
            }
        } else {
            try {
                reader = new PdfReader(inputFile);
            } catch (IOException e) {
                throw new PdfConvertException(
                        "Pdf add water Sign error, pdf file is [" + inputFile + "]," + e.getMessage(), e);
            }
        }

        Image logoImage = null;
        try {
            logoImage = Image.getInstance(logoImageFile);
        } catch (BadElementException e) {
            throw new PdfConvertException("Logo image file [" + logoImageFile + "] is invalid," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException("Logo image file [" + logoImageFile + "] is not exists," + e.getMessage(), e);
        }

        Image dateImage = null;
        try {
            dateImage = getDateImage(dateImageFile, date);
        } catch (BadElementException e) {
            throw new PdfConvertException("Date image file [" + dateImage + "] is invalid," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException("Date image file [" + dateImage + "] is not exists," + e.getMessage(), e);
        }

        PdfStamper stamper = null;
        try {
            logger.info("Add water sign to pdf, pdf file is [{}], output file is [{}]", inputFile, outputFile);
            PdfReader.unethicalreading = true;
            stamper = new PdfStamper(reader, new FileOutputStream(outputFile));
            int pages = reader.getNumberOfPages();// 页数
            for (int i = 1; i < pages + 1; i++) {
                PdfContentByte under = stamper.getOverContent(i);
                float width = reader.getPageSize(i).getWidth();
                float height = reader.getPageSize(i).getHeight();
                dateImage.setAbsolutePosition((float) (width * widthRatio), (float) (height * heightRatio));// 图片位置
                logoImage.setAbsolutePosition((float) (width * (widthRatio + 0.02)),
                        (float) (height * (heightRatio + 0.05)));// 图片位置
                under.addImage(logoImage);// 添加水印图片
                under.addImage(dateImage);// 添加日期水印图片
            }
            stamper.close();
        } catch (DocumentException e) {
            throw new PdfConvertException(
                    "PDF打水印失败, pdf file [" + inputFile + "], image file [" + logoImageFile + "]," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException(
                    "PDF打水印失败, pdf file [" + inputFile + "], image file [" + logoImageFile + "]," + e.getMessage(), e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (DocumentException e) {
                    logger.error(e.getMessage(), e);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (null != reader) {
                reader.close();
            }
        }
    }

    /**
     * pdf的合并
     *
     * @param target  pdf数组
     * @param sources 转化后存放的路径
     * @throws PdfConvertException
     */
    public static void concat(String target, String... sources) throws PdfConvertException {
        Document document = new Document();
        PdfCopy copy = null;
        PdfReader reader = null;
        try {
            copy = new PdfCopy(document, new FileOutputStream(target));
            document.open();
            int n;
            logger.info("Concat pdfs, pdf files is {}, output file is [{}]", sources, target);
            if (null != sources && sources.length > 0) {
                for (int i = 0; i < sources.length; i++) {
                    reader = new PdfReader(sources[i]);
                    n = reader.getNumberOfPages();
                    for (int page = 0; page < n; ) {
                        copy.addPage(copy.getImportedPage(reader, ++page));
                    }
                    copy.freeReader(reader);
                    reader.close();
                }
            }
        } catch (DocumentException e) {
            throw new PdfConvertException(
                    "合并PDF失败, target file [" + target + "], sources files " + Joiner.on(",").skipNulls().join(sources) + "," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException(
                    "合并PDF失败, target file [" + target + "], sources files " + Joiner.on(",").skipNulls().join(sources) + "," + e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (copy != null) {
                copy.close();
            }
            document.close();
        }
    }

    /**
     * pdf 合并时 itext PDF header signature not found 解决
     * http://stackoverflow.com/questions/5183973/jms-textmessage-itext-pdf-header-signature-not-found
     *
     * @param target
     * @param sources
     * @throws PdfConvertException
     */
    public static void pdfMerge(String target, String... sources) throws PdfConvertException {
        if (null == sources || sources.length <= 0) {
            logger.info(String.format("while pdf merge but source  is  empty "));
            return;
        }
        Document document = new Document();
        PdfWriter writer = null;
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(target)));
            writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            for (int i = 0; i < sources.length; i++) {
                PdfReader reader = new PdfReader(sources[i]);
                for (int j = 1; j <= reader.getNumberOfPages(); j++) {
                    document.newPage();
                    // import the page from source pdf
                    PdfImportedPage page = writer.getImportedPage(reader, j);
                    // add the page to the destination pdf
                    cb.addTemplate(page, 0, 0);
                }
            }
        } catch (DocumentException e) {
            throw new PdfConvertException(
                    "合并PDF失败, target file [" + target + "], sources files " + Joiner.on(",").skipNulls().join(sources) + "," + e.getMessage(), e);
        } catch (IOException e) {
            throw new PdfConvertException(
                    "合并PDF失败, target file [" + target + "], sources files " + Joiner.on(",").skipNulls().join(sources) + "," + e.getMessage(), e);
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != document) {
                document.close();
            }
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws PdfConvertException {
        pdfMerge("f:\\1234.pdf", new String[]{"F:\\精益创业（美） 埃里克·莱斯 .pdf", "F:\\Documents\\产品文档\\无忧保全\\无忧保全产品介绍V1.pdf"});
    }

    // public static void startService(){
    // DefaultOfficeManagerConfiguration cofiguration=new
    // DefaultOfficeManagerConfiguration();
    // String home=getSysConfig().get(SysConfig.KEY_OPEN_OFFICE_HOME);
    // cofiguration.setOfficeHome(home);//
    // String port =getSysConfig().get(SysConfig.KEY_OPEN_OFFICE_PORT);
    // //String port ="8100";
    // cofiguration.setPortNumber(Integer.parseInt(port));//
    // cofiguration.setTaskExecutionTimeout(60*5*1000L);//设置任务执行超时为5分钟
    // cofiguration.setTaskQueueTimeout(1000 * 60 * 60 * 24L);//设置任务队列超时为24小时
    //
    // officeManager=cofiguration.buildOfficeManager();
    // logger.info(String.format("openoffice.home is %s openoffice.port is %s",
    // home,port));
    // officeManager.start();
    // }

//	/**
//	 * @param sourcePdf
//	 *            pdf路径
//	 * @param targetSwf
//	 *            swf输出路径
//	 * @throws PdfConvertException
//	 */
//	public static void pdfToSwf(String sourcePdf, String targetSwf) throws PdfConvertException {
//		if (StringUtils.isBlank(targetSwf)) {
//			logger.error("swf的输出路径不能为空");
//			throw new PdfConvertException("swf的输出路径不能为空");
//		}
//
//		File file = new File(sourcePdf);
//		if (!file.exists()) {
//			logger.error("要生产swf的pdf文件不存在:" + sourcePdf);
//			throw new PdfConvertException("要生产swf的pdf文件不存在:" + sourcePdf);
//		}
//
//		String outputDir = targetSwf.substring(0, targetSwf.lastIndexOf("/"));
//		File output = new File(outputDir);
//		if (!output.exists()) {
//			output.mkdirs();
//		}
//
//		// String command = commanddir + " -t " + pdfpath + " -o " +
//		// swfoutputdir + " -s flashversion=9"
//		// + " -s languageDir=" + languagedir;
//		String command = String.format("%s -t %s -o %s -s flashversion=9 -s languageDir=%s", commanddir, sourcePdf,
//				targetSwf, languagedir);
//
//		logger.info("Begin pdf to swf, command is [{}]", command);
//
//		Runtime rt = Runtime.getRuntime();
//		int count = 0;
//		int result = -1;
//		PdfConvertException pdfConvertException = null;
//		while (count < 5) {// 失败重试5次
//			try {
//				result = pdfToSwfInner(rt, command);
//				if (result == 0) {// 成功
//					break;
//				}
//			} catch (PdfConvertException ex) {
//				pdfConvertException = ex;
//			}
//			count++;
//		}
//		if (result != 0) {
//			throw new PdfConvertException("Pdf to swf wrong,result:[" + result + "]," + command);
//		} else if (pdfConvertException != null) {
//			throw new PdfConvertException("Pdf to swf wrong," + pdfConvertException.getMessage() + command,
//					pdfConvertException);
//		}
//	}

    private static int pdfToSwfInner(Runtime rt, String command) throws PdfConvertException {
        Process process = null;
        try {
            process = rt.exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (br.readLine() != null) {
                ;//
            }
            BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while (br2.readLine() != null) {
                ;//
            }
            process.waitFor();
            return process.exitValue();
        } catch (IOException ex) {
            throw new PdfConvertException(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            throw new PdfConvertException(ex.getMessage(), ex);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    public static int getPdfPages(String file) throws IOException {
        int pdfPages;
        PdfReader reader;
        reader = new PdfReader(file);
        pdfPages = reader.getNumberOfPages();
        return pdfPages;
    }

    public static List<String> pdfToImage(String pdfPath) throws PdfConvertException {
        // 路径前缀
        String prefixPath = pdfPath.substring(0, pdfPath.lastIndexOf("/") + 1);
        // pdf的文件名
        String pdfName = pdfPath.substring(pdfPath.lastIndexOf("/") + 1, pdfPath.lastIndexOf("."));
        // 返回结果路径列表
        List<String> imgPathList = new ArrayList<String>();
        org.icepdf.core.pobjects.Document document = new org.icepdf.core.pobjects.Document();
        try {
            document.setFile(pdfPath);
        } catch (Exception ex) {
            logger.error("pdf convert image error:", ex.getMessage());
            throw new PdfConvertException("Pdf to image error,pdf [" + pdfPath + "] " + ex.getMessage(), ex);
        }
        float rotation = 0f;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            BufferedImage image = (BufferedImage) document.getPageImage(i - 1, GraphicsRenderingHints.SCREEN,
                    Page.BOUNDARY_CROPBOX, rotation, 1.5f);
            sb.setLength(0);
            sb.append(prefixPath).append(pdfName).append("_").append(i).append(".png");// 每页生成的图片路径
            imgPathList.add(sb.toString());
            RenderedImage rendImage = image;
            try {
                File file = new File(sb.toString());
                ImageIO.write(rendImage, "PNG", file);
            } catch (IOException e) {
                logger.error("pdf convert image error:", e.getMessage());
                throw new PdfConvertException("Pdf to image error,pdf [" + pdfPath + "] " + e.getMessage(), e);
            } finally {
                document.dispose();
            }
            image.flush();
        }
        return imgPathList;
    }

    public static List<String> pdfToImage(InputStream pdfStream, String pdfPath) throws PdfConvertException {
        return pdfToImage(pdfStream, pdfPath, null);
    }

    /**
     * @param sourcePdfStream
     * @param targetImgNamePrefix
     * @param targetRootDir
     * @throws PdfConvertException
     */
    public static List<String> pdfToImage(InputStream sourcePdfStream, String targetImgNamePrefix, String targetRootDir)
            throws PdfConvertException {
        // 路径前缀
        String prefixPath = targetImgNamePrefix.substring(0, targetImgNamePrefix.lastIndexOf("/") + 1);
        // pdf的文件名
        String pdfName = targetImgNamePrefix.substring(targetImgNamePrefix.lastIndexOf("/") + 1,
                targetImgNamePrefix.lastIndexOf("."));
        // 返回结果路径列表
        List<String> imgPathList = new ArrayList<String>();

        int len = 0;
        StringBuilder sb = new StringBuilder();
        // PdfDecoder pdfDecoder = new PdfDecoder(true);
        org.icepdf.core.pobjects.Document document = new org.icepdf.core.pobjects.Document();
        FileUtils.mkdirs(PathUtils.concat(targetRootDir, targetImgNamePrefix));// 创建输出目录
        FileOutputStream fos = null;
        try {
            document.setInputStream(sourcePdfStream, null);// 打开pdf
            len = document.getNumberOfPages();
            // pdfDecoder.openPdfArray();
            // len = pdfDecoder.getPageCount();// pdf页数
            BufferedImage image = null;
            RenderedImage rendImage = null;
            for (int i = 1; i < len + 1; i++) {
                sb.setLength(0);
                sb.append(prefixPath).append(pdfName).append("_").append(i).append(".png");// 每页生成的图片路径
                imgPathList.add(sb.toString());
                image = (BufferedImage) document.getPageImage(i - 1, GraphicsRenderingHints.SCREEN,
                        Page.BOUNDARY_TRIMBOX, 0f, 1.5f);
                ;

                rendImage = image;
                // pdfDecoder.setPageParameters(1.0f, i);
                // BufferedImage img = pdfDecoder.getPageAsImage(i);
                if (targetRootDir != null) {
                    fos = new FileOutputStream(PathUtils.concat(targetRootDir, sb.toString()));
                } else {
                    fos = new FileOutputStream(sb.toString());
                }
                JPEGEncodeParam jep = JPEGCodec.getDefaultJPEGEncodeParam(image);
                if (jep != null) {
                    jep.setQuality(0.92f, true);
                    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(fos, jep);
                    encoder.encode(image);
                } else {
                    File jpgFile = null;
                    if (targetRootDir != null) {
                        jpgFile = new File(PathUtils.concat(targetRootDir, sb.toString()));
                    } else {
                        jpgFile = new File(sb.toString());
                    }

                    ImageIO.write(rendImage, "PNG", jpgFile);
                    image.flush();
                }
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (IOException e) {
            throw new PdfConvertException("Pdf to image error,pdf [" + targetImgNamePrefix + "] " + e, e);
        } catch (Exception e) {
            throw new PdfConvertException("Pdf to image error,pdf [" + targetImgNamePrefix + "] " + e, e);
        } finally {
            document.dispose();
        }
        return imgPathList;
    }

    private static Image getDateImage(String bgpath, String date) throws IOException, BadElementException {
        BufferedImage bid = null;

        File imageFile = new File(bgpath); // 原始图片文件
        bid = ImageIO.read(imageFile);

        Font font = new Font("微软雅黑", Font.PLAIN, 18);
        Graphics2D g2 = (Graphics2D) bid.getGraphics();
        g2.setFont(font);
        g2.setPaint(Color.RED);
        g2.setBackground(Color.white);
        g2.setPaint(Color.RED);
        g2.drawString(date, 15, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bid, "png", baos);
        byte[] bytes = baos.toByteArray();
        return Image.getInstance(bytes);
    }

    private static byte[] inputStreamToByte(InputStream in) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int count = -1;
        while ((count = in.read(data, 0, 1024)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return outStream.toByteArray();
    }
}
