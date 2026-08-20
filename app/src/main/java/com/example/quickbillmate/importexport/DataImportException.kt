package com.example.quickbillmate.importexport

/** 数据导入通用异常（文件读取、JSON 解析、行校验失败）。 */
open class DataImportException(message: String) : Exception(message)
