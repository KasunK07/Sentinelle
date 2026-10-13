package SentinellePay.Sentinelle.service;

import org.springframework.web.multipart.MultipartFile;

import SentinellePay.Sentinelle.dto.BinUploadResponseDto;


public interface BinMasterService {


	BinUploadResponseDto uploadBinMaster(MultipartFile file);

	int getBinCount();


	boolean isBinLoaded();
}