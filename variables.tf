variable "project-name" {
    description = "Project title"
    type = string
    default = ""
}

variable "aws-region" {
}

variable "envvariable" {
}

variable "CIDR-Block-VPC" {
    description = "CIDR block for the VPC" #Classless inter domain routing
    type = string
}

variable "ssh-port" {
    description = "ssh port"
    type = number
}

variable "dns-resolution" {
    description = "DNS resolution"
    type = bool
}

variable "cidr-private-subnets" {
    description = "CIDR for private subnets"
    type = list(string)
    default = []
}

variable "private-subnet" {
    description = "Private subnet map"
    type = map(string)
}