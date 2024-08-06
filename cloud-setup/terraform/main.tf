terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~>5"
    }
    random = {
      source  = "hashicorp/random"
      version = "~>3.5"
    }
  }
  backend "s3" {
    bucket = "antons-devops-lessons" 
    key    = "zwiftoffline.project"   
    region = "us-east-1"
  }
  required_version = ">= 1.7"
}

provider "aws" {
  region = "us-east-1"
}